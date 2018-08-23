require 'spec_helper'
require_relative 'graphql_helper'

describe 'requests' do
  def get_requests(result)
    result[:data][:budget_periods]
      .flat_map { |el| el.fetch(:main_categories) }
      .flat_map { |el| el.fetch(:categories) }
      .flat_map { |el| el.fetch(:requests) }
  end

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

  context 'filter for states' do
    let :q do
      <<-GRAPHQL
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
    end

    before :example do
      @bp_requesting_phase = \
        FactoryBot.create(:budget_period,
                          inspection_start_date: Date.tomorrow,
                          end_date: Date.today + 1.week)

      @bp_inspection_phase = \
        FactoryBot.create(:budget_period,
                          inspection_start_date: Date.today,
                          end_date: Date.today + 1.week)

      @bp_past = \
        FactoryBot.create(:budget_period,
                          inspection_start_date: Date.today - 1.month, 
                          end_date: Date.yesterday)

      @bp_past = \
        FactoryBot.create(:budget_period,
                          inspection_start_date: Date.today - 1.week,
                          end_date: Date.yesterday)

      @bp_future = \
        FactoryBot.create(:budget_period,
                          inspection_start_date: Date.today + 1.month,
                          end_date: Date.today + 2.months)

      @requester = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: @requester.id)

      @category = FactoryBot.create(:category)
      @inspector = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector,
                        category_id: @category.id,
                        user_id: @inspector.id)

      # with approved entity entered already
      @request_new_requesting_phase_with_partially_approved_quantity = \
        FactoryBot.create(:request,
                          user_id: @requester.id,
                          budget_period_id: @bp_requesting_phase.id,
                          requested_quantity: 2,
                          approved_quantity: 1)

      # with no approved entity entered already
      @request_new_requesting_phase = \
        FactoryBot.create(:request,
                          user_id: @requester.id,
                          budget_period_id: @bp_requesting_phase.id,
                          requested_quantity: 2)

      # no approved quantity entered
      @request_new_inspection_phase = \
        FactoryBot.create(:request,
                          user_id: @requester.id,
                          budget_period_id: @bp_inspection_phase.id,
                          requested_quantity: 2)

      # approved quantity entered
      @request_partially_approved_inspection_phase = \
        FactoryBot.create(:request,
                          user_id: @requester.id,
                          budget_period_id: @bp_inspection_phase.id,
                          requested_quantity: 2,
                          approved_quantity: 1)

      @request_denied_inspection_phase = \
        FactoryBot.create(:request,
                          category_id: @category.id,
                          user_id: @requester.id,
                          budget_period_id: @bp_inspection_phase.id,
                          requested_quantity: 1,
                          approved_quantity: 0)

      @request_denied_past = \
        FactoryBot.create(:request,
                          category_id: @category.id,
                          user_id: @requester.id,
                          budget_period_id: @bp_past.id,
                          requested_quantity: 1,
                          approved_quantity: 0)

      @budget_periods = [@bp_requesting_phase, @bp_inspection_phase, @bp_past]
      @bp_ids = @budget_periods.map(&:id)
    end

    context 'requester' do
      example 'ignored states `approved`, `partially_approved` and `denied` for inspection phase' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['APPROVED', 'PARTIALLY_APPROVED', 'DENIED'] }
        result = query(q, @requester.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(1)
        r_ids = requests.map { |r| r[:id] }
        expect(r_ids).to include @request_denied_past.id
      end

      example '`new` in requesting phase irrespective of approved quantity' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['NEW'] }
        result = query(q, @requester.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(2)
        r_ids = requests.map { |r| r[:id] }
        expect(r_ids).to include @request_new_requesting_phase.id
        expect(r_ids).to include @request_new_requesting_phase_with_partially_approved_quantity.id
      end

      example '`in_approval` in inspection phase irrespective of approved quantity' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['IN_APPROVAL'] }
        result = query(q, @requester.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(3)
        r_ids = requests.map { |r| r[:id] }
        expect(r_ids).to include @request_new_inspection_phase.id
        expect(r_ids).to include @request_partially_approved_inspection_phase.id
        expect(r_ids).to include @request_denied_inspection_phase.id
      end
    end

    context 'inspector' do
      example 'tbd' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['NEW', 'APPROVED', 'DENIED'] }
        result = query(q, @inspector.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(4)
        r_ids = requests.map { |r| r[:id] }
        expect(r_ids).to include @request_new_requesting_phase.id
        expect(r_ids).to include @request_new_inspection_phase.id
        expect(r_ids).to include @request_denied_inspection_phase.id
        expect(r_ids).to include @request_denied_past.id
      end
    end
  end
end
