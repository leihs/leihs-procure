require 'spec_helper'
require_relative 'graphql_helper'

describe 'budget periods' do
  it 'mutation' do
    budget_periods_before = [
      { name: 'bp_to_delete' },
      { name: 'bp_1' }
    ]
    budget_periods_before.each do |data|
      FactoryBot.create(:budget_period, data)
    end

    #############################################################################
    
    now = DateTime.now
    new_inspection_start_date_1 = DateTime.new(now.year + 1, 6, 1)
    new_end_date_1 = DateTime.new(now.year + 1, 12, 1)
    new_inspection_start_date_2 = DateTime.new(now.year + 2, 6, 1)
    new_end_date_2 = DateTime.new(now.year + 2, 12, 1)

    query = <<-GRAPHQL
        mutation {
          budget_periods (
            input_data: [
              { id: "#{BudgetPeriod.find(name: 'bp_1').id}",
                name: "bp_1_new_name",
                inspection_start_date: "#{new_inspection_start_date_1}",
                end_date: "#{new_end_date_1}" },
              { id: null,
                name: "new_bp",
                inspection_start_date: "#{new_inspection_start_date_2}",
                end_date: "#{new_end_date_2}" }
            ]
          ) {
            name
          } 
        }
    GRAPHQL

    response = graphql_client.query(query)

    # sorted after `inspection_start_date DESC`
    expect(response.to_h).to be == {
      'data' => {
        'budget_periods' => [
          { 'name' => 'new_bp' },
          { 'name' => 'bp_1_new_name' }
        ]
      }
    }

    budget_periods_after = [
      { name: 'new_bp',
        inspection_start_date: new_inspection_start_date_2,
        end_date: new_end_date_2 },
      { name: 'bp_1_new_name',
        inspection_start_date: new_inspection_start_date_1, 
        end_date: new_end_date_1 }
    ]
    expect(BudgetPeriod.count).to be == budget_periods_after.count
    budget_periods_after.each do |data|
      expect(BudgetPeriod.find(data)).to be
    end
  end
end
