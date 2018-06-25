require 'spec_helper'
require_relative 'graphql_helper'

describe 'requests' do
  context 'editing request should not yield any error' do
    before :example do
      @q = <<-GRAPHQL
        query RequestForEdit($requestIds: [ID!]!) {
          requests(id: $requestIds) {
            ...RequestFieldsForShow
          }
        }

        fragment RequestFieldsForShow on Request {
          ...RequestFieldsForIndex

          category {
            id
            name
          }
          budget_period {
            id
          }

          article_name {
            ...RequestFieldString
          }
          supplier {
            ...RequestFieldString
          }
          receiver {
            ...RequestFieldString
          }
          organization {
            id
          }

          # FIXME: price fields
          # price_cents {
          #   ...RequestFieldString
          # }
          # price_currency {
          #   ...RequestFieldString
          # }

          requested_quantity {
            ...RequestFieldInt
          }
          approved_quantity {
            ...RequestFieldInt
          }
          order_quantity {
            ...RequestFieldInt
          }

          # FIXME: priority
          # priority {
          #   ...RequestFieldString
          # }
          # FIXME: state
          # state {
          #   ...RequestFieldString
          # }

          article_number {
            ...RequestFieldString
          }
          motivation {
            ...RequestFieldString
          }

          # FIXME: replacement
          # replacement {
          #   ...RequestFieldBoolean
          # }

          room {
            read
            write
            value {
              id
              name
              building {
                id
                name
              }
            }
          }

          inspection_comment {
            ...RequestFieldString
          }
          # TODO: priority_inspector

          # TODO: attachments

          # FIXME: accounting_type
          # accounting_type {
          #   ...RequestFieldString
          # }
          # internal_order_id
        }

        fragment RequestFieldsForIndex on Request {
          id

          category {
            id
            name
          }
          budget_period {
            id
          }

          article_name {
            value
          }
          receiver {
            value
          }
          organization {
            id
          }

          # FIXME: price fields
          # price_cents {
          #   value
          # }
          # price_currency {
          #   value
          # }

          requested_quantity {
            value
          }
          approved_quantity {
            value
          }
          order_quantity {
            value
          }

          replacement {
            value
          }

          # FIXME: priority
          # priority {
          #   value
          # }

          # FIXME: state
          # state {
          #   value
          # }
        }

        fragment RequestFieldString on RequestFieldString { value, read, write }
        fragment RequestFieldInt on RequestFieldInt { value, read, write }
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

    after :example do
      variables = { requestIds: ["#{@request.id}"] }
      result = query(@q, @user.id, variables)
      expect(result['errors']).not_to be
    end
  end
end
