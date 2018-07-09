require 'spec_helper'
require_relative 'graphql_helper'

describe 'requests' do
  context 'editing request should not yield any error' do
    before :example do
      @q = <<-GRAPHQL
        query RequestForEdit($requestIds: [ID!]!) {
          requests(id: $requestIds) {
            accounting_type {
              read
              value
              write
            }
            accounting_type {
              read
              value
              write
            }
            article_name {
              read
              value
              write
            }
            article_number {
              read
              value
              write
            }
            budget_period {
              read
              value {
                id
              }
              write
            }
            category {
              read
              value {
                id
              }
              write
            }
            cost_center {
              read
              value 
              write
            }
            general_ledger_account {
              read
              value 
              write
            }
            id
            inspection_comment {
              read
              value
              write
            }
            inspector_priority {
              read
              value
              write
            }
            internal_order_number {
              read
              value
              write
            }
            model {
              read
              value {
                id
              }
              write
            }
            motivation {
              read
              value
              write
            }
            order_quantity {
              read
              value
              write
            }
            organization {
              read
              value {
                id
              }
              write
            }
            price_cents {
              read
              value
              write
            }
            price_cents {
              read
              value
              write
            }
            priority {
              read
              value
              write
            }
            procurement_account {
              read
              value 
              write
            }
            receiver {
              read
              value
              write
            }
            replacement {
              read
              value
              write
            }
            requested_quantity {
              read
              value
              write
            }
            room {
              read
              value {
                id
              }
              write
            }
            state {
              read
              value
              write
            }
            supplier {
              read
              value {
                id
              }
              write
            }
            supplier_name {
              read
              value
              write
            }
            user {
              read
              value {
                id
              }
              write
            }
          }
        }
      GRAPHQL
    end

    it 'as requester' do
      @user = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: @user.id)
      @request = FactoryBot.create(:request, user_id: @user.id)
    end

    it 'as admin' do
      @user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: @user.id)
      @request = FactoryBot.create(:request)
    end

    it 'as inspector' do
      @user = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
                        user_id: @user.id,
                        category_id: category.id)
      @request = FactoryBot.create(:request,
                                   category_id: category.id)
    end

    it 'as viewer' do
      @user = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_viewer,
                        user_id: @user.id,
                        category_id: category.id)
      @request = FactoryBot.create(:request,
                                   category_id: category.id)
    end

    after :example do
      variables = { requestIds: ["#{@request.id}"] }
      result = query(@q, @user.id, variables)
      expect(result['errors']).not_to be
    end
  end
end
