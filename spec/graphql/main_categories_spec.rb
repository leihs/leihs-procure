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

      budget_limits_before = [
        { main_category: { name: 'main_cat_1' },
          budget_period: { name: 'budget_period_1' },
          amount_cents: 50 },
        { main_category: { name: 'main_cat_1' },
          budget_period: { name: 'budget_period_2' },
          amount_cents: 100 }
      ]
      budget_limits_before.each do |data|
        FactoryBot.create(
          :budget_limit,
          main_category_id: MainCategory.find(data[:main_category]).id,
          budget_period_id: BudgetPeriod.find(data[:budget_period]).id,
          amount_cents: data[:amount_cents]
        )
      end

      #############################################################################

      query = <<-GRAPHQL
        mutation {
          main_categories (
            input_data: [
              { id: null,
                name: "new_main_cat",
                budget_limits: [
                  { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_1').id}",
                    amount_cents: 111 },
                  { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_2').id}",
                    amount_cents: 222 }
                ]
              },
              { id: "#{MainCategory.find(name: 'main_cat_1').id}",
                name: "main_cat_1",
                budget_limits: [
                  { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_1').id}",
                    amount_cents: 333 },
                  { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_2').id}",
                    amount_cents: 444 }
                ]
              }
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

      #############################################################################

      expect(BudgetPeriod.count).to be == budget_periods_before.count
      budget_periods_before.each do |data|
        expect(BudgetPeriod.find(data)).to be
      end

      main_categories_after = [
        { name: 'new_main_cat' },
        { name: 'main_cat_1' }
      ]
      expect(MainCategory.count).to be == main_categories_after.count
      main_categories_after.each do |data|
        expect(MainCategory.find(data)).to be
      end

      budget_limits_after = [
        { main_category: { name: 'new_main_cat' },
          budget_period: { name: 'budget_period_1' },
          amount_cents: 111 },
        { main_category: { name: 'new_main_cat' },
          budget_period: { name: 'budget_period_2' },
          amount_cents: 222 },
        { main_category: { name: 'main_cat_1' },
          budget_period: { name: 'budget_period_1' },
          amount_cents: 333 },
        { main_category: { name: 'main_cat_1' },
          budget_period: { name: 'budget_period_2' },
          amount_cents: 444 }
      ]
      expect(BudgetLimit.count).to be == budget_limits_after.count
      budget_limits_after.each do |data|
        bl = BudgetLimit.find(
          main_category_id: MainCategory.find(data[:main_category]).id,
          budget_period_id: BudgetPeriod.find(data[:budget_period]).id,
          amount_cents: data[:amount_cents]
        )
        expect(bl).to be
      end
    end
  end
end
