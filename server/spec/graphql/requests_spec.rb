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
    expect(result).to eq({
      'data' => {
        'requests' => [
          { 'id' => request.id
          }
        ]
      }
    })
  end

  example 'query for dashboard' do
    user = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: user.id)
    request = FactoryBot.create(:request, user_id: user.id)

    q = <<-GRAPHQL
      query RequestsIndexFiltered(
        $budgetPeriods: [ID]
        $categories: [ID]
        $search: String
        $priority: [Priority]
        $inspectory_priority: [InspectorPriority]
        $onlyOwnRequests: Boolean
      ) {
        budget_periods(id: $budgetPeriods) {
          id
          # name
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
                inspectory_priority: $inspectory_priority
                requested_by_auth_user: $onlyOwnRequests
              ) {
                id
              }
            }
          }
        }
      }
    GRAPHQL

    result = query(q, user.id).deep_symbolize_keys

    expect(result).to eq({
      data: {
        budget_periods: [{
          id: request.budget_period_id,
          main_categories: [{
            id: Category.find(id: request.category_id).main_category_id,
            categories: [{
              id: request.category_id,
              requests: [{ id: request.id }]
            }]
          }]
        }]
      }
    })
  end
end
