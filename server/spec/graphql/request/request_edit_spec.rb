require "spec_helper"
require_relative "../graphql_helper"

describe "requests" do
  context "editing request should not yield any error" do
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
                department {
                  id
                }
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
            state 
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
          budget_periods(whereRequestsCanBeMovedTo: $requestIds) {
            id
          }
        }
      GRAPHQL

      @bp_past = FactoryBot.create(:budget_period, :past)
      @bp_requesting_phase = FactoryBot.create(:budget_period, :requesting_phase)
      @bp_inspection_phase = FactoryBot.create(:budget_period, :inspection_phase)
    end

    it "as requester" do
      @user = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: @user.id)
      @request = FactoryBot.create(:request,
        user_id: @user.id,
        budget_period_id: @bp_requesting_phase.id)
    end

    it "as admin" do
      @user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: @user.id)
      @request = FactoryBot.create(:request,
        budget_period_id: @bp_requesting_phase.id)
    end

    it "as inspector" do
      @user = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
        user_id: @user.id,
        category_id: category.id)
      @request = FactoryBot.create(:request,
        category_id: category.id,
        budget_period_id: @bp_requesting_phase.id)
    end

    it "as viewer" do
      @user = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_viewer,
        user_id: @user.id,
        category_id: category.id)
      @request = FactoryBot.create(:request,
        category_id: category.id,
        budget_period_id: @bp_requesting_phase.id)
    end

    after :example do
      variables = {requestIds: [@request.id.to_s]}
      result = query(@q, @user.id, variables)
      expect(result["errors"]).not_to be
      expect(result["data"]["budget_periods"].map { |bp| bp["id"] })
        .to match_array [@bp_requesting_phase.id, @bp_inspection_phase.id]
    end
  end

  context "change user" do
    example "not writable but readable" do
      user = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: user.id)
      request = FactoryBot.create(:request, user_id: user.id)

      q = <<-GRAPHQL
        query RequestForEdit($requestIds: [ID!]!) {
          requests(id: $requestIds) {
            user {
              read
              write
            }
          }
        }
      GRAPHQL

      variables = {requestIds: [request.id.to_s]}
      result = query(q, user.id, variables)
      expect(result).to eq(
        {"data" => {
          "requests" => [
            {"user" =>
              {"read" => true,
               "write" => false}}
          ]
        }}
      )
    end

    example "readable/writable" do
      inspector = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
        user_id: inspector.id,
        category_id: category.id)

      admin = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: admin.id)

      request = FactoryBot.create(:request, category_id: category.id)

      q = <<-GRAPHQL
        query RequestForEdit($requestIds: [ID!]!) {
          requests(id: $requestIds) {
            user {
              read
              write
            }
          }
        }
      GRAPHQL

      [inspector, admin].each do |user|
        variables = {requestIds: [request.id.to_s]}
        result = query(q, user.id, variables)
        expect(result).to eq(
          {"data" => {
            "requests" => [
              {"user" =>
                {"read" => true,
                 "write" => true}}
            ]
          }}
        )
      end
    end
  end
end
