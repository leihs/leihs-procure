require 'spec_helper'
require_relative 'graphql_helper'

describe 'price sums' do
  let :user do
    user = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: user.id)
    user
  end

  it 'computes correctly' do
    budget_period_I = FactoryBot.create(:budget_period,
                                        name: 'budget_period_I')

    # ----------------------------------------------------------------------

    main_category_1 = FactoryBot.create(:main_category,
                                        name: 'main_category_1')

    category_1_A = FactoryBot.create(:category,
                                     main_category_id: main_category_1.id,
                                     name: 'category_1_A')
    request_I_1_A = FactoryBot.create(:request,
                                      category_id: category_1_A.id,
                                      budget_period_id: budget_period_I.id,
                                      user_id: user.id,
                                      price_cents: 100,
                                      requested_quantity: 1,
                                      approved_quantity: 0)

    category_1_B = FactoryBot.create(:category,
                                     main_category_id: main_category_1.id,
                                     name: 'category_1_B')
    request_I_1_B = FactoryBot.create(:request,
                                      category_id: category_1_B.id,
                                      budget_period_id: budget_period_I.id,
                                      user_id: user.id,
                                      price_cents: 100,
                                      requested_quantity: 1,
                                      approved_quantity: 1,
                                      order_quantity: 0)

    category_1_C = FactoryBot.create(:category,
                                     main_category_id: main_category_1.id,
                                     name: 'category_1_C')
    request_I_1_C = FactoryBot.create(:request,
                                      category_id: category_1_C.id,
                                      budget_period_id: budget_period_I.id,
                                      user_id: user.id,
                                      price_cents: 100,
                                      requested_quantity: 1)

    # ----------------------------------------------------------------------
    #
    main_category_2 = FactoryBot.create(:main_category,
                                        name: 'main_category_2')

    category_2_A = FactoryBot.create(:category,
                                     main_category_id: main_category_2.id,
                                     name: 'category_2_A')
    request_I_2_A = FactoryBot.create(:request,
                                      category_id: category_2_A.id,
                                      budget_period_id: budget_period_I.id,
                                      user_id: user.id,
                                      price_cents: 100,
                                      requested_quantity: 1,
                                      approved_quantity: 1,
                                      order_quantity: 1)

    category_2_B = FactoryBot.create(:category,
                                     main_category_id: main_category_2.id,
                                     name: 'category_2_B')
    # priority 'high'
    request_I_2_B = FactoryBot.create(:request,
                                      category_id: category_2_B.id,
                                      budget_period_id: budget_period_I.id,
                                      user_id: user.id,
                                      priority: 'high',
                                      price_cents: 100,
                                      requested_quantity: 1)

    category_2_C = FactoryBot.create(:category,
                                     main_category_id: main_category_2.id,
                                     name: 'category_2_C')
    # not visible for the user
    request_I_2_C = FactoryBot.create(:request,
                                      category_id: category_2_C.id,
                                      budget_period_id: budget_period_I.id,
                                      price_cents: 100,
                                      requested_quantity: 1)

    category_2_D = FactoryBot.create(:category,
                                     main_category_id: main_category_2.id,
                                     name: 'category_2_D')
    # from a category not set in filter
    request_I_2_D = FactoryBot.create(:request,
                                      category_id: category_2_D.id,
                                      budget_period_id: budget_period_I.id,
                                      user_id: user.id,
                                      price_cents: 100,
                                      requested_quantity: 1)

    # =============================================================================

    budget_period_II = FactoryBot.create(:budget_period,
                                         name: 'budget_period_II')

    # from a budget period not set in filter
    request_II_1_A = FactoryBot.create(:request,
                                       category_id: category_1_A.id,
                                       budget_period_id: budget_period_II.id,
                                       user_id: user.id,
                                       price_cents: 100,
                                       requested_quantity: 1)

    query = <<-GRAPHQL
      query RequestsIndexFiltered(
        $budgetPeriods: [ID]
        $categories: [ID]
        $priority: [Priority]
        $onlyOwnRequests: Boolean
      ) {
        budget_periods(id: $budgetPeriods) {
          id
          total_price_cents
          main_categories {
            id
            total_price_cents
            categories(id: $categories) {
              id
              total_price_cents
              requests(
                priority: $priority
                requested_by_auth_user: $onlyOwnRequests
              ) {
                id
              }
            }
          }
        }
      }
    GRAPHQL

    expected_result = {
      data: {
        budget_periods: [
          { id: budget_period_I.id,
            total_price_cents: '200',
            main_categories: [
              { id: main_category_1.id,
                total_price_cents: '100',
                categories: [
                  { id: category_1_A.id,
                    total_price_cents: '0',
                    requests: [
                      { id: request_I_1_A.id }
                    ]
                  },
                  { id: category_1_B.id,
                    total_price_cents: '0',
                    requests: [
                      { id: request_I_1_B.id }
                    ]
                  },
                  { id: category_1_C.id,
                    total_price_cents: '100',
                    requests: [
                      { id: request_I_1_C.id }
                    ]
                  }
                ]
              },
              { id: main_category_2.id,
                total_price_cents: '100',
                categories: [
                  { id: category_2_A.id,
                    total_price_cents: '100',
                    requests: [
                      { id: request_I_2_A.id }
                    ]
                  },
                  { id: category_2_B.id,
                    total_price_cents: '0',
                    requests: []
                  },
                  { id: category_2_C.id,
                    total_price_cents: '0',
                    requests: []
                  }
                ]
              }
            ]
          }
        ]
      }
    }

    variables = {
      budgetPeriods: [budget_period_I.id],
      categories: [category_1_A.id,
                   category_1_B.id,
                   category_1_C.id,
                   category_2_A.id,
                   category_2_B.id,
                   category_2_C.id],
      priority: ['NORMAL']
    }

    result = query(query, user.id, variables).deep_symbolize_keys
    expect(result).to eq(expected_result)
  end
end
