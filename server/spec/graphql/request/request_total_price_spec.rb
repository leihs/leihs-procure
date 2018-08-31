require 'spec_helper'
require_relative '../graphql_helper'

describe 'request total price' do
  before :example do
    @user_id = FactoryBot.create(:admin).user_id

    @q = <<-GRAPHQL
      query RequestForEdit($requestIds: [ID!]!) {
        requests(id: $requestIds) {
          id
          total_price_cents
        }
      }
    GRAPHQL
  end

  it 'check all possible combinations' do
    cases = [
      {
        price_cents: 100,
        requested_quantity: 0,
        total_price_cents: '0' },
      {
        price_cents: 100,
        requested_quantity: 42,
        total_price_cents: '4200' },
      {
        price_cents: 100,
        requested_quantity: 42,
        approved_quantity: 23,
        total_price_cents: '2300'
      },
      {
        price_cents: 100,
        requested_quantity: 42,
        approved_quantity: 23,
        order_quantity: 5,
        total_price_cents: '500'
      },
      {
        price_cents: 250000,
        requested_quantity: 30,
        approved_quantity: nil,
        order_quantity: nil,
        total_price_cents: '7500000'
      },
      {
        price_cents: 250000,
        requested_quantity: 30,
        approved_quantity: 20,
        order_quantity: nil,
        total_price_cents: '5000000'
      },
      {
        price_cents: 250000,
        requested_quantity: 30,
        approved_quantity: 20,
        order_quantity: 0,
        total_price_cents: '0'
      }
    ]

    cases.each_with_index do |c, i|
      puts "case: #{i + 1}"
      puts c

      request = FactoryBot.create(
        :request,
        requested_quantity: c[:requested_quantity],
        approved_quantity: c[:approved_quantity],
        order_quantity: c[:order_quantity],
        price_cents: c[:price_cents]
      )

      variables = { requestIds: ["#{request.id}"] }
      result = query(@q, @user_id, variables).deep_symbolize_keys
      request_data = result[:data][:requests].first
      expect(request_data[:id]).to eq(request.id)
      expect(request_data[:total_price_cents]).to eq(c[:total_price_cents])
    end
  end
end
