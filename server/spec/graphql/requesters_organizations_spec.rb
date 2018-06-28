require 'spec_helper'
require_relative 'graphql_helper'

describe 'requesters organizations' do
  context 'mutation' do
    before :example do
      users_before = [
        { firstname: 'user_1' },
        { firstname: 'user_2' },
        { firstname: 'user_3' },
        { firstname: 'user_4' },
        { firstname: 'user_5' },
        { firstname: 'user_6' },
        { firstname: 'user_7' }
      ]
      users_before.each do |data|
        FactoryBot.create(:user, data)
      end

      #############################################################################

      organizations_before = [
        { name: 'dep_I' },
        { name: 'dep_II' },
        { name: 'dep_III' },
        { name: 'dep_IV' },
        { name: 'dep_V' },
        { name: 'dep_VI' },
        { name: 'org_A', parent: { name: 'dep_I' } },
        { name: 'org_B', parent: { name: 'dep_II' } },
        { name: 'org_C', parent: { name: 'dep_III' } },
        { name: 'org_D', parent: { name: 'dep_IV' } },
        { name: 'org_E', parent: { name: 'dep_V' } },
        { name: 'org_F', parent: { name: 'dep_VI' } }
      ]
      organizations_before.each do |data|
        parent = data[:parent] && Organization.find(data[:parent])
        FactoryBot.create(:organization, name: data[:name], parent: parent)
      end

      #############################################################################

      @requesters_organizations_before = [
        { user: { firstname: 'user_1' }, organization: { name: 'org_A' } },
        { user: { firstname: 'user_2' }, organization: { name: 'org_B' } },
        { user: { firstname: 'user_3' }, organization: { name: 'org_C' } },
        { user: { firstname: 'user_4' }, organization: { name: 'org_D' } },
        { user: { firstname: 'user_5' }, organization: { name: 'org_E' } },
        { user: { firstname: 'user_6' }, organization: { name: 'org_E' } }
      ]
      @requesters_organizations_before.each do |data|
        FactoryBot.create(
          :requester_organization,
          user: User.find(data[:user]),
          organization: Organization.find(data[:organization])
        )
      end

      #############################################################################

      requests_before = [
        { organization: { name: 'org_F' } }
      ]
      requests_before.each do |data|
        FactoryBot.create(
          :request,
          organization_id: Organization.find(data[:organization]).id
        )
      end

      #############################################################################

      users_filters_before = [
        { user: { firstname: 'user_1' } },
        { user: { firstname: 'user_2' } },
        { user: { firstname: 'user_3' } },
        { user: { firstname: 'user_4' } },
        { user: { firstname: 'user_5' } }
      ]
      users_filters_before.each do |data|
        FactoryBot.create(:user_filter, user_id: User.find(data[:user]).id)
      end

      #############################################################################

      @q = <<-GRAPHQL
        mutation {
          requesters_organizations (
            input_data: [
              { user_id: "#{User.find(firstname: 'user_2').id}",
                department: "dep_II",
                organization: "org_B" },

              { user_id: "#{User.find(firstname: 'user_3').id}",
                department: "dep_X",
                organization: "org_C" },

              { user_id: "#{User.find(firstname: 'user_4').id}",
                department: "dep_IV",
                organization: "org_X" },

              { user_id: "#{User.find(firstname: 'user_5').id}",
                department: "dep_XI",
                organization: "org_Y" },

              { user_id: "#{User.find(firstname: 'user_7').id}",
                department: "dep_V",
                organization: "org_E" }
            ]
          ) {
            user {
              id
            } 
            organization {
              name
            }
            department {
              name
            }
          } 
        }
      GRAPHQL
    end

    it 'returns error for unauthorized user' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector,
                        user_id: user.id)
      result = query(@q, user.id)

      expect(result['data']['requesters_organizations']).to be_empty
      expect(result['errors'].first['exception'])
        .to be == 'UnauthorizedException'

      RequesterOrganization
        .all
        .zip(@requesters_organizations_before)
        .each do |ro1, ro2|
          expect(User.find(id: ro1.user_id).firstname)
            .to be == ro2[:user][:firstname]
          expect(Organization.find(id: ro1.organization_id).name)
            .to be == ro2[:organization][:name]
        end
    end

    it 'recreates all and does the necessary cleanup' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)

      result = query(@q, user.id)

      expect(result).to eq({
        'data' => {
          'requesters_organizations' => [
            { 'user' => { 'id' => "#{User.find(firstname: 'user_2').id}" },
              'organization' => { 'name' => 'org_B' },
              'department' => { 'name' => 'dep_II' }},

            { 'user' => { 'id' => "#{User.find(firstname: 'user_3').id}" },
              'organization' => { 'name' => 'org_C' },
              'department' => { 'name' => 'dep_X' }},

            { 'user' => { 'id' => "#{User.find(firstname: 'user_4').id}" },
              'organization' => { 'name' => 'org_X' },
              'department' => { 'name' => 'dep_IV' }},

            { 'user' => { 'id' => "#{User.find(firstname: 'user_5').id}" },
              'organization' => { 'name' => 'org_Y' },
              'department' => { 'name' => 'dep_XI' }},

            { 'user' => { 'id' => "#{User.find(firstname: 'user_7').id}" },
              'organization' => { 'name' => 'org_E' },
              'department' => { 'name' => 'dep_V' }}
          ]
        }
      })

      #############################################################################

      organizations_after = [
        { name: 'org_B', parent: { name: 'dep_II' } },
        { name: 'org_C', parent: { name: 'dep_X' } },
        { name: 'org_X', parent: { name: 'dep_IV' } },
        { name: 'org_Y', parent: { name: 'dep_XI' } },
        { name: 'org_E', parent: { name: 'dep_V' } },
        { name: 'org_F', parent: { name: 'dep_VI' } }
      ]
      expect(Organization.count).to be == organizations_after.count * 2
      organizations_after.each do |data|
        parent = Organization.find(data[:parent])
        expect(Organization.find(name: data[:name], parent: parent)).to be
      end

      #############################################################################

      requesters_organizations_after = [
        { user: { firstname: 'user_2' }, organization: { name: 'org_B' } },
        { user: { firstname: 'user_3' }, organization: { name: 'org_C' } },
        { user: { firstname: 'user_4' }, organization: { name: 'org_X' } },
        { user: { firstname: 'user_5' }, organization: { name: 'org_Y' } },
        { user: { firstname: 'user_7' }, organization: { name: 'org_E' } }
      ]
      expect(RequesterOrganization.count)
        .to be == requesters_organizations_after.count
      requesters_organizations_after.each do |data|
        user = User.find(data[:user])
        organization = Organization.find(data[:organization])
        expect(RequesterOrganization.find(user: user, organization: organization))
          .to be
      end

      #############################################################################

      users_filters_after = [
        { user: { firstname: 'user_2' } },
        { user: { firstname: 'user_3' } },
        { user: { firstname: 'user_4' } },
        { user: { firstname: 'user_5' } }
      ]
      expect(UserFilter.count).to be == users_filters_after.count
      users_filters_after.each do |data|
        user = User.find(data[:user])
        expect(UserFilter.find(user_id: user.id)).to be
      end
    end
  end
end
