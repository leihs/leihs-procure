require 'spec_helper'
require_relative 'graphql_helper'

describe 'admins' do
  context 'query' do
    context 'authorization' do 
      context 'unauthorized user' do
        it 'returns empty data and an error' do
          user = FactoryBot.create(:user)
          FactoryBot.create(:category_inspector, user_id: user.id)
          FactoryBot.create(:admin)

          q = <<-GRAPHQL
            query {
              admins { 
                id
              }
            }
          GRAPHQL

          result = query(q, user.id)
          expect(result['data']['admins']).to be_empty
          expect(result['errors'].first['exception'])
            .to be == 'UnauthorizedException'
        end
      end
    end
  end

  context 'mutation' do
    context 'authorization' do 
      context 'unauthorized user' do
        it 'returns empty data and an error' do
          user_1 = FactoryBot.create(:user)
          FactoryBot.create(:category_inspector, user_id: user_1.id)
          user_2 = FactoryBot.create(:user)
          admin_1 = FactoryBot.create(:user)
          FactoryBot.create(:admin, user_id: admin_1.id)

          q = <<-GRAPHQL
            mutation {
              admins (
                input_data: [
                  { user_id: "#{user_2.id}" }
                ]
              ) { id } 
            }
          GRAPHQL

          result = query(q, user_1.id)
          expect(result['data']['admins']).to be_empty
          expect(result['errors'].first['exception'])
            .to be == 'UnauthorizedException'

          expect(Admin.count).to be == 1
          expect(Admin.first.user_id).to be == admin_1.id
        end
      end
    end

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

      q = <<-GRAPHQL
        mutation {
          admins (
            input_data: [
              { user_id: "#{User.find(firstname: 'admin_2').id}" },
              { user_id: "#{User.find(firstname: 'user_1').id}" }
            ]
          ) { id } 
        }
      GRAPHQL

      result = query(q, User.find(firstname: 'admin_2').id)

      expect(result).to eq({
        'data' => {
          'admins' => [
            { 'id' => "#{User.find(firstname: 'admin_2').id}" },
            { 'id' => "#{User.find(firstname: 'user_1').id}" },
          ]
        }
      })

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
