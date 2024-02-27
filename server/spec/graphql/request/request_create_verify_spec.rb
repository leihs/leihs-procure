require 'spec_helper'
require_relative '../graphql_helper'
require_relative 'request_helper'

describe 'request' do
  let(:minimal_input) do
    {
      article_name: 'new request',
      budget_period: FactoryBot.create(:budget_period).id,
      category: FactoryBot.create(:category).id,
      requested_quantity: 1,
      room: FactoryBot.create(:room).id,
      motivation: Faker::Lorem.sentence,
    }
  end

  let :q do
    <<-GRAPHQL
      mutation createRequest($input: CreateRequestInput) {
        create_request(input_data: $input) {
          id
          article_name {
            value
          }
        }
      }
    GRAPHQL
  end

  it 'returns id of created request' do
    initially_existing_request = FactoryBot.create(
      :request,
      id: '12345678-1234-1234-1234-abcdef123456',
      article_name: 'already existing request',
      price_cents: 2300,
      category_id: FactoryBot.create(:category).id,
      requested_quantity: 1,
      motivation: Faker::Lorem.sentence,
    )

    viewer = FactoryBot.create(:user)
    category = FactoryBot.create(:category)
    FactoryBot.create(:category_viewer,
                      user_id: viewer.id,
                      category_id: category.id)

    variables = {
      input: minimal_input.merge({
                                   article_name: 'new request',
                                   attachments: [],
                                   supplier_name: 'OBike'
                                 })
    }

    @category = FactoryBot.create(:category)
    @auth_user = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: @auth_user.id)
    FactoryBot.create(:category_inspector,
                      user_id: @auth_user.id,
                      category_id: @category.id)

    result = query(q, @auth_user.id, variables).deep_symbolize_keys

    expect(result[:data][:create_request][:id]).not_to eq(initially_existing_request.id)
    expect(result[:data][:create_request][:id]).to eq(Request.find(article_name: 'new request').id)
  end
end
