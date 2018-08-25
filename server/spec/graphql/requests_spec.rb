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
          budget_periods: BudgetPeriod.order(:end_date).reverse.map do |bp|
            {
              id: bp.id,
              name: bp.name,
              main_categories: MainCategory.order(:name).map do |main_cat|
                {
                  id: main_cat.id,
                  categories: Category.where(main_category_id: main_cat.id).order(:name).map do |cat|
                    {
                      id: cat.id,
                      requests: Request.where(
                        category_id: cat.id, budget_period_id: bp.id
                      ).map do |r|
                        { id: r.id }
                      end
                    }
                  end
                }
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
end
