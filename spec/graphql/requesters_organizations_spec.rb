require 'spec_helper'
require_relative 'graphql_helper'

describe 'requesters organizations' do
  context 'mutation' do
    it 'recreates all and does the necessary cleanup' do
      # o1 & d1 to re retained due to r1
      d1 = FactoryBot.create(:department)
      o1 = FactoryBot.create(:organization, parent_id: d1.id)
      r1 = FactoryBot.create(:request, organization_id: o1.id)
      # o2 & d2 to be retained due to ro2
      d2 = FactoryBot.create(:department)
      o2 = FactoryBot.create(:organization, parent_id: d2.id)
      # unused o3 & d3, to be deleted both
      d3 = FactoryBot.create(:department)
      o3 = FactoryBot.create(:organization)

      # ro1 to be deleted
      ro1 = FactoryBot.create(:requester_organization)
      # ro2 to be retained
      ro2 = FactoryBot.create(:requester_organization, organization_id: o2.id)
      # ro3, department to be modified
      ro3_dep_new_name = Faker::Commerce.department
      ro3 = FactoryBot.create(:requester_organization)
      # ro4, organization to be modified
      ro4_org_new_name = Faker::Commerce.department
      ro4 = FactoryBot.create(:requester_organization)
      # ro5, organization & department to be modified
      ro5_dep_new_name = Faker::Commerce.department
      ro5_org_new_name = Faker::Commerce.department
      ro5 = FactoryBot.create(:requester_organization)

      # uf1 to be deleted with ro1
      uf1 = FactoryBot.create(:user_filter, user_id: ro1.user_id)
      # uf2 to be retained with ro2
      uf2 = FactoryBot.create(:user_filter, user_id: ro2.user_id)
      # uf3 to be retained with ro3
      uf3 = FactoryBot.create(:user_filter, user_id: ro3.user_id)
      # uf4 to be retained with ro4
      uf4 = FactoryBot.create(:user_filter, user_id: ro4.user_id)
      # uf5 to be retained with ro5
      uf5 = FactoryBot.create(:user_filter, user_id: ro5.user_id)

      # TEST DATA BEFORE
      #
      # procurement_requesters_organizations:
      # +----------+------------------+
      # | user     | organization     |
      # +----------+------------------+
      # | ro1.user | ro1.organization |
      # | ro2.user | o2.id            |
      # | ro3.user | ro3.organization |
      # | ro4.user | ro4.organization |
      # | ro5.user | ro5.organization |
      # +----------+------------------+
      #
      # procurement_organizations:
      # +---------+-----------+
      # | name    | parent_id |
      # +---------+-----------+
      # | d1.name |           |
      # | o1.name | d1.id     |
      # | d2.name |           |
      # | o2.name | d2.id     |
      # | d3.name |           |
      # | o3.name | d3.id     |
      # +---------+-----------+
      #
      # procurement_requests:
      # +-------+-----------------+
      # | id    | organization_id |
      # +-------+-----------------+
      # | r1.id | o1.id           |
      # +-------+-----------------+
      #
      # procurement_users_filters:
      # +--------+-------------+
      # | id     | user_id     |
      # +--------+-------------+
      # | uf1.id | ro1.user_id |
      # +--------+-------------+
      # | uf2.id | ro2.user_id |
      # +--------+-------------+
      # | uf3.id | ro3.user_id |
      # +--------+-------------+
      # | uf4.id | ro4.user_id |
      # +--------+-------------+
      # | uf5.id | ro5.user_id |
      # +--------+-------------+
      
      response = graphql_client.query <<-GRAPHQL
        mutation {
          requesters_organizations (
            input_data: [
              { user_id: "#{ro2.user_id}",
                department: "#{d2.name}",
                organization: "#{o2.name}" },

              { user_id: "#{ro3.user_id}",
                department: "#{ro3_dep_new_name}",
                organization: "#{ro3.organization.name}" },

              { user_id: "#{ro4.user_id}",
                department: "#{ro4.organization.parent.name}",
                organization: "#{ro4_org_new_name}" },

              { user_id: "#{ro5.user_id}",
                department: "#{ro5_dep_new_name}",
                organization: "#{ro5_org_new_name}" }
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

      expect(response.to_h).to be == {
        'data' => {
          'requesters_organizations' => [
            { 'user' => { 'id' => ro2.user.id },
              'organization' => { 'name' => o2.name },
              'department' => { 'name' => o2.parent.name }},

            { 'user' => { 'id' => ro3.user.id },
              'organization' => { 'name' => ro3.organization.name },
              'department' => { 'name' => ro3_dep_new_name }},

            { 'user' => { 'id' => ro4.user.id },
              'organization' => { 'name' => ro4_org_new_name },
              'department' => { 'name' => ro4.organization.parent.name }},

            { 'user' => { 'id' => ro5.user.id },
              'organization' => { 'name' => ro5_org_new_name },
              'department' => { 'name' => ro5_dep_new_name }}
          ]
        }
      }
    end
  end
end

