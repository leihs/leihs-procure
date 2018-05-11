require 'spec_helper'
require_relative 'graphql_helper'

describe 'admins' do
  context 'query' do
    context 'authorization' do 
      context 'unauthorized user' do
        it 'returns empty data and an error' do
          user = FactoryBot.create(:user)
          FactoryBot.create(:admin)
        end
      end
    end
  end

  context 'mutation' do
    it 'recreates all' do

      users_before = [
        { firstname: 'user_1' }
      ]
      users_before.each do |data|
        FactoryBot.create(:user, data)
      end

      admins_before = [
        { firstname: 'admin_1' },
        { firstname: 'admin_2' }
      ]
      admins_before.each do |data|
        FactoryBot.create(:admin, user_id: FactoryBot.create(:user, data).id)
      end

      #############################################################################

      query = <<-GRAPHQL
        mutation {
          admins (
            input_data: [
              { user_id: "#{User.find(firstname: 'admin_2').id}" },
              { user_id: "#{User.find(firstname: 'user_1').id}" }
            ]
          ) { id } 
        }
      GRAPHQL

      response = graphql_client(User.find(firstname: 'admin_2').id).query(query)

      expect(response.to_h).to be == {
        'data' => {
          'admins' => [
            { 'id' => "#{User.find(firstname: 'admin_2').id}" },
            { 'id' => "#{User.find(firstname: 'user_1').id}" },
          ]
        }
      }

      #############################################################################

      admins_after = [
        { firstname: 'admin_2' },
        { firstname: 'user_1' }
      ]
      expect(Admin.count).to be == admins_after.count
      admins_after.each do |data|
        expect(Admin.find(user_id: User.find(data).id)).to be
      end
    end
  end
end

