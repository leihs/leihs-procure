require 'spec_helper'
require_relative 'graphql_helper'

describe 'main categories' do
  context 'mutation' do
    it 'updates all together with budget limits, sub-categories and their inspectors' do

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

      budget_periods_before = [
        { name: 'budget_period_1' },
        { name: 'budget_period_2' }
      ]
      budget_periods_before.each do |data|
        FactoryBot.create(:budget_period, data)
      end

      #############################################################################
      main_categories_before = [
        { name: 'main_cat_1' }
      ]
      main_categories_before.each do |data|
        FactoryBot.create(:main_category, name: data[:name])
      end

      #############################################################################

      query = <<-GRAPHQL
        mutation {
          main_categories (
            input_data: [
              { id: null, name: "new_main_cat", budget_limits: [] },
              { id: "#{MainCategory.find(name: 'main_cat_1')}", name: "main_cat_1", budget_limits: [] }
            ]
          ) {
            name
          } 
        }
      GRAPHQL

      response = graphql_client.query(query)

      expect(response.to_h).to be == {
        'data' => {
          'main_categories' => [
            { "name" => 'main_cat_1' },
            { "name" => 'new_main_cat' }
          ]
        }
      }
    end
  end
end
