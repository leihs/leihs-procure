require 'spec_helper'
require_relative 'graphql_helper'

describe 'budget periods' do
  context 'query' do
    example 'returns no error' do
      FactoryBot.create(:request)

      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)

      q = <<-GRAPHQL
        query {
          budget_periods {
            id
            name
            inspection_start_date
            end_date
            total_price_cents_requested_quantities
            total_price_cents_approved_quantities
            total_price_cents_order_quantities
          }
        }
      GRAPHQL

      result = query(q, user.id)
      expect(result['errors']).to be_nil
    end

    example 'authorizes total_price_cents_* query path' do
      FactoryBot.create(:request)

      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector, user_id: user.id)

      [:requested, :approved, :order].each do |qty_type|
        tp = "total_price_cents_#{qty_type}_quantities"

        q = <<-GRAPHQL
          query {
            budget_periods {
              id
              #{tp}
            }
          }
        GRAPHQL

        result = query(q, user.id)
        expect(result['data']['budget_periods'].first[tp]).to be_nil
        expect(result['errors'].first['exception']).to be == 'UnauthorizedException'
      end
    end
  end

  context 'mutation' do
    before :example do
      budget_periods_before = [{ name: 'bp_to_delete' },
                               { name: 'bp_1' }]

      budget_periods_before.each do |data|
        FactoryBot.create(:budget_period, data)
      end

      budget_limits_before = [{ budget_period: { name: 'bp_to_delete' } },
                              { budget_period: { name: 'bp_1' } }]

      budget_limits_before.each do |data|
        FactoryBot.create(
          :budget_limit,
          budget_period_id: BudgetPeriod.find(data[:budget_period]).id
        )
      end

      now = DateTime.now
      @new_inspection_start_date_1 = DateTime.new(now.year + 1, 6, 1)
      @new_end_date_1 = DateTime.new(now.year + 1, 12, 1)
      @new_inspection_start_date_2 = DateTime.new(now.year + 2, 6, 1)
      @new_end_date_2 = DateTime.new(now.year + 2, 12, 1)

      @q = <<-GRAPHQL
        mutation {
          budget_periods (
            input_data: [
              { id: "#{BudgetPeriod.find(name: 'bp_1').id}",
                name: "bp_1_new_name",
                inspection_start_date: "#{@new_inspection_start_date_1}",
                end_date: "#{@new_end_date_1}" },
              { id: null,
                name: "new_bp",
                inspection_start_date: "#{@new_inspection_start_date_2}",
                end_date: "#{@new_end_date_2}" }
            ]
          ) {
            name
          } 
        }
      GRAPHQL
    end

    #############################################################################
    
    it 'returns error for unauthorized user' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector, user_id: user.id)

      result = query(@q, user.id)

      expect(result['data']['budget_periods']).to be_empty
      expect(result['errors'].first['exception'])
        .to be == 'UnauthorizedException'

      expect(BudgetPeriod.all.map(&:name)).to be == ['bp_to_delete', 'bp_1']
    end


    it 'updates successfully for an authorized user' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)

      result = query(@q, user.id)

      # sorted after `inspection_start_date DESC`
      expect(result).to eq({
        'data' => {
          'budget_periods' => [
            { 'name' => 'new_bp' },
            { 'name' => 'bp_1_new_name' }
          ]
        }
      })

      budget_periods_after = [
        { name: 'new_bp',
          inspection_start_date: @new_inspection_start_date_2,
          end_date: @new_end_date_2 },
        { name: 'bp_1_new_name',
          inspection_start_date: @new_inspection_start_date_1, 
          end_date: @new_end_date_1 }
      ]
      expect(BudgetPeriod.count).to be == budget_periods_after.count
      budget_periods_after.each do |data|
        expect(BudgetPeriod.find(data)).to be
      end

      budget_limits_after = [
        { budget_period: { name: 'bp_1_new_name' } }
      ]
      budget_limits_after.each do |data|
        FactoryBot.create(
          :budget_limit,
          budget_period_id: BudgetPeriod.find(data[:budget_period]).id
        )
      end
    end
  end
end
