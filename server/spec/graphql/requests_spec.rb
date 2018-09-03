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
      ['2001', '2002', '2003'].each do |name|
        FactoryBot.create(:budget_period, name: name)
      end
      ['main cat X', 'main cat Y', 'main cat Z'].each do |name|
        FactoryBot.create(:main_category, name: name)
      end
      FactoryBot.create(:category,
                        name: 'cat A',
                        main_category_id: MainCategory.find(name: 'main cat X').id)
      FactoryBot.create(:category,
                        name: 'cat B',
                        main_category_id: MainCategory.find(name: 'main cat Y').id)
      FactoryBot.create(:category,
                        name: 'cat C',
                        main_category_id: MainCategory.find(name: 'main cat Z').id)

      @requests = BudgetPeriod.all.map do |bp|
        Category.all.map do |cat|
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
          $organizations: [ID]
          $search: String
          $priority: [Priority]
          $inspector_priority: [InspectorPriority]
          $onlyOwnRequests: Boolean
          $withTotalSums: Boolean = false
        ) {
          budget_periods(id: $budgetPeriods) {
            id
            name
            total_price_cents @include(if: $withTotalSums)
            # inspection_start_date
            # end_date

            main_categories {
              id
              total_price_cents @include(if: $withTotalSums)
              # name
              # image_url

              categories(id: $categories) {
                id
                total_price_cents @include(if: $withTotalSums)
                # name

                requests(
                  search: $search
                  organization_id: $organizations
                  priority: $priority
                  inspector_priority: $inspector_priority
                  requested_by_auth_user: $onlyOwnRequests
                ) {
                  id
                  price_cents @include(if: $withTotalSums) { value }
                  total_price_cents @include(if: $withTotalSums)
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

    example 'empty array filter arguments do not crash' do
      variable_keys_to_try = [
        :budgetPeriods,
        :organizations,
        :categories,
        :priority,
        :inspector_priority,
        :state,
      ]

      variable_keys_to_try.each do |var_name|
        result = query(@query, @user.id, {var_name => [], withTotalSums: true})
        expect(result['errors']).to be_nil
      end
    end

    example 'total sums of more than 32-bit-cents do not crash' do
      # TODO: 'price and total sums of more than 32-bit-cents do not crash'
      #        => price_cents: (2**33),
      @requests = BudgetPeriod.all.map do |bp|
        Category.all.map do |cat|
          10.times do
            FactoryBot.create(
              :request,
              user_id: @user.id,
              budget_period_id: bp.id,
              category_id: cat.id,
              price_cents: 4600*100,
              requested_quantity: 4000
            )
          end
        end.flatten
      end

      result = query(@query, @user.id, {withTotalSums: true})
      bp_totals = result['data']['budget_periods'].map {|bp| bp['total_price_cents']}
      # expect at least 1 sum over 32bit size AND no errors
      expect(bp_totals.any? {|n| n.to_i > 2**33}).to be
      expect(result['errors']).to be_nil
    end

    example 'filter for budget periods and categories' do
      bps = BudgetPeriod.where(name: ['2003', '2002'])
      cats = Category.where(name: ['cat A', 'cat B'])

      variables = {
        budgetPeriods: bps.map(&:id),
        categories: cats.map(&:id),
        priority: ['NORMAL']
      }

      expected_result = {
        data: {
          budget_periods: bps.order(:name).reverse.map do |bp|
            {
              id: bp.id,
              name: bp.name,
              main_categories: MainCategory.order(:name).map do |main_cat|
                {
                  id: main_cat.id,
                  categories: Category.where(id: cats.map(&:id), main_category_id: main_cat.id).map do |cat|
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
                  state
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

      @request_approved_inspection_phase = \
        FactoryBot.create(:request,
                          user_id: @requester.id,
                          budget_period_id: @bp_inspection_phase.id,
                          requested_quantity: 2,
                          approved_quantity: 2)

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
      context 'ignored states `approved`, `partially_approved` and `denied` for inspection phase' do
        example '1' do
          variables = { budgetPeriods: @bp_ids,
                        states: ['APPROVED', 'PARTIALLY_APPROVED', 'DENIED'] }
          result = query(q, @requester.id, variables).deep_symbolize_keys
          requests = get_requests(result)
          expect(requests.count).to eq(1)
          expect(requests).to include Hash[:id, @request_denied_past.id,
                                           :state, 'DENIED']
        end

        example '2' do
          variables = { budgetPeriods: @bp_ids,
                        states: ['NEW', 'PARTIALLY_APPROVED', 'DENIED'] }
          result = query(q, @requester.id, variables).deep_symbolize_keys
          requests = get_requests(result)
          expect(requests.count).to eq(3)
          expect(requests).to include Hash[:id, @request_new_requesting_phase.id,
                                           :state, 'NEW']
          expect(requests).to include Hash[:id, @request_new_requesting_phase_with_partially_approved_quantity.id,
                                           :state, 'NEW']
          expect(requests).to include Hash[:id, @request_denied_past.id,
                                           :state, 'DENIED']
        end
      end

      example '`new` in requesting phase irrespective of approved quantity' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['NEW'] }
        result = query(q, @requester.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(2)
        expect(requests).to include Hash[:id, @request_new_requesting_phase.id,
                                         :state, 'NEW']
        expect(requests).to include Hash[:id, @request_new_requesting_phase_with_partially_approved_quantity.id,
                                         :state, 'NEW']
      end

      example '`in_approval` in inspection phase irrespective of approved quantity' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['IN_APPROVAL'] }
        result = query(q, @requester.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(4)
        expect(requests).to include Hash[:id, @request_new_inspection_phase.id,
                                         :state, 'IN_APPROVAL']
        expect(requests).to include Hash[:id, @request_approved_inspection_phase.id,
                                         :state, 'IN_APPROVAL']
        expect(requests).to include Hash[:id, @request_partially_approved_inspection_phase.id,
                                         :state, 'IN_APPROVAL']
        expect(requests).to include Hash[:id, @request_denied_inspection_phase.id,
                                         :state, 'IN_APPROVAL']
      end
    end

    context 'inspector' do
      example 'tbd' do
        variables = { budgetPeriods: @bp_ids,
                      states: ['NEW', 'APPROVED', 'PARTIALLY_APPROVED', 'DENIED'] }
        result = query(q, @inspector.id, variables).deep_symbolize_keys
        requests = get_requests(result)
        expect(requests.count).to eq(7)
        expect(requests).to include Hash[:id, @request_new_requesting_phase.id,
                                         :state, 'NEW']
        expect(requests).to include Hash[:id, @request_new_inspection_phase.id,
                                         :state, 'NEW']
        expect(requests).to include Hash[:id, @request_new_requesting_phase_with_partially_approved_quantity.id,
                                         :state, 'PARTIALLY_APPROVED']
        expect(requests).to include Hash[:id, @request_approved_inspection_phase.id,
                                         :state, 'APPROVED']
        expect(requests).to include Hash[:id, @request_partially_approved_inspection_phase.id,
                                         :state, 'PARTIALLY_APPROVED']
        expect(requests).to include Hash[:id, @request_denied_inspection_phase.id,
                                         :state, 'DENIED']
        expect(requests).to include Hash[:id, @request_denied_past.id,
                                         :state, 'DENIED']
      end
    end
  end
end
