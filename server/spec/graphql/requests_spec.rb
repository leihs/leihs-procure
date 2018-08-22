require 'spec_helper'
require_relative 'graphql_helper'

describe 'requests' do
  it 'gets data' do
    user = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: user.id)
    request = FactoryBot.create(:request, user_id: user.id)

    q = <<-GRAPHQL
      query {
        requests {
          id
        }
      }
    GRAPHQL
    result = query(q, user.id)
    expect(result).to eq('data' => {
                           'requests' => [
                             { 'id' => request.id
                             }
                           ]
                         })
  end

  context 'query for dashboard: filtered requests' do
    before do
      @user = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: @user.id)
      budget_periods = Array.new(3) do |i|
        FactoryBot.create(:budget_period, name: "200#{i}")
      end
      categories = Array.new(3) do |_i|
        mc = FactoryBot.create(:main_category)
        FactoryBot.create(:category, main_category_id: mc.id)
      end
      @requests = budget_periods.map do |bp|
        categories.map do |cat|
          FactoryBot.create(
            :request,
            user_id: @user.id,
            budget_period_id: bp.id,
            category_id: cat.id
          )
        end.flatten
      end

      @query = <<-GRAPHQL
        query RequestsIndexFiltered(
          $budgetPeriods: [ID]
          $categories: [ID]
          $search: String
          $priority: [Priority]
          $inspector_priority: [InspectorPriority]
          $onlyOwnRequests: Boolean
        ) {
          budget_periods(id: $budgetPeriods) {
            id
            name
            # inspection_start_date
            # end_date

            main_categories {
              id
              # name
              # image_url

              categories(id: $categories) {
                id
                # name

                requests(
                  # TODO: organization_id: $organizations #
                  search: $search
                  priority: $priority
                  inspector_priority: $inspector_priority
                  requested_by_auth_user: $onlyOwnRequests
                ) {
                  id
                }
              }
            }
          }
        }
      GRAPHQL
    end

    example 'no filter arguments' do
      variables = {}
      expected_result = {
        data: {
          budget_periods: BudgetPeriod.all.reverse.map do |bp|
            {
              id: bp.id,
              name: bp.name,
              main_categories: Category.all.map do |cat|
                {
                  id: cat.main_category_id,
                  categories: [
                    {
                      id: cat.id,
                      requests: Request.where(
                        category_id: cat.id, budget_period_id: bp.id
                      ).map do |r|
                        { id: r.id }
                      end
                    }
                  ] }
              end
            }
          end
        }
      }

      result = query(@query, @user.id, variables).deep_symbolize_keys
      expect(result).to eq(expected_result)
    end

    example 'filter for budget periods and categories' do
      variables = {
        budgetPeriods: BudgetPeriod.first(2).map(&:id),
        categories: Category.first(2).map(&:id),
        priority: ['NORMAL']
      }

      expected_result = {
        data: {
          budget_periods: BudgetPeriod.first(2).reverse.map do |bp|
            {
              id: bp.id,
              name: bp.name,
              main_categories: Category.all.map.with_index do |cat, i|
                {
                  id: cat.main_category_id,
                  categories: (i > 1) ? [] : [
                    {
                      id: cat.id,
                      requests: Request.where(
                        category_id: cat.id, budget_period_id: bp.id
                      ).map do |r|
                        { id: r.id }
                      end
                    }
                  ] }
              end
            }
          end
        }
      }

      result = query(@query, @user.id, variables).deep_symbolize_keys
      expect(result).to eq(expected_result)
    end
  end

  example 'filter for states' do
    requester = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: requester.id)

    bp_inspection_phase = FactoryBot.create(:budget_period,
                                            inspection_start_date: Date.today,
                                            end_date: Date.today + 1.week)
    bp_past = FactoryBot.create(:budget_period,
                                inspection_start_date: Date.today - 1.week,
                                end_date: Date.yesterday)
    bp_future = FactoryBot.create(:budget_period,
                                  inspection_start_date: Date.today + 1.month,
                                  end_date: Date.today + 2.months)

    # request_new_inspection_phase = FactoryBot.create(:request,
    #                                                  user_id: requester.id,
    #                                                  budget_period_id: bp_inspection_phase.id,
    #                                                  requested_quantity: 1)

    request_denied_inspection_phase = FactoryBot.create(:request,
                                                        user_id: requester.id,
                                                        budget_period_id: bp_inspection_phase.id,
                                                        requested_quantity: 1,
                                                        approved_quantity: 0)

    q = <<-GRAPHQL
      query RequestsIndexFiltered(
        $budgetPeriods: [ID]
        $states: [State]
      ) {
        budget_periods(id: $budgetPeriods) {
          id
          main_categories {
            id
            categories {
              id
              requests(state: $states) {
                id
              }
            }
          }
        }
      }
    GRAPHQL

    variables = { budgetPeriods: [bp_inspection_phase.id],
                  states: ['APPROVED', 'PARTIALLY_APPROVED', 'DENIED'] }
    result = query(q, requester.id, variables).deep_symbolize_keys
    requests = \
      result[:data]
      .flat_map { |el| el.fetch(:budget_periods) }
      .flat_map { |el| el.fetch(:main_categories) }
      .flat_map { |el| el.fetch(:categories) }
      .flat_map { |el| el.fetch(:requests) }
    expect(requests).to be_empty
  end
end
