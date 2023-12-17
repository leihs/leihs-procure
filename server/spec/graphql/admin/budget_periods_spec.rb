require 'spec_helper'
require_relative '../graphql_helper'

describe 'budget periods' do
  context 'query' do
    example 'returns no error' do
      FactoryBot.create(:request)

      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)

      q = <<-GRAPHQL
        query {
          budget_periods {
            id
            name
            inspection_start_date
            end_date
            total_price_cents_new_requests
          }
        }
      GRAPHQL

      result = query(q, user.id)
      expect(result['errors']).to be_nil
    end

    example 'correct calculation of total price new requests' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)
      bp = FactoryBot.create(:budget_period)
      [
        # INCLUDE
        { requested_quantity: 1,
          approved_quantity: nil,
          order_quantity: nil },
        # INCLUDE
        { requested_quantity: 1,
          approved_quantity: nil,
          order_quantity: 1 },
        # DON'T INCLUDE
        { requested_quantity: 1,
          approved_quantity: 1,
          order_quantity: nil },
        # DON'T INCLUDE
        { requested_quantity: 1,
          approved_quantity: 1,
          order_quantity: 1 }
      ].each do |qts|
        FactoryBot.create(
          :request,
          qts.merge(
            budget_period_id: bp.id,
            price_cents: 100
          )
        )
      end

      q = <<-GRAPHQL
        query {
          budget_periods(id: ["#{bp.id}"]) {
            id
            total_price_cents_new_requests
          }
        }
      GRAPHQL

      result = query(q, user.id)
      expect(
        result['data']['budget_periods'].first['total_price_cents_new_requests']
      ).to eq '200'
    end

    example 'correct calculation of total price any approved requests' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)
      bp = FactoryBot.create(:budget_period)
      [
        # DON'T INCLUDE
        { requested_quantity: 1,
          approved_quantity: nil,
          order_quantity: nil },
        # INCLUDE (approved_quantity)
        { requested_quantity: 1,
          approved_quantity: 0,
          order_quantity: nil },
        # INCLUDE (approved_quantity)
        { requested_quantity: 2,
          approved_quantity: 1,
          order_quantity: nil },
        # INCLUDE (approved_quantity)
        { requested_quantity: 2,
          approved_quantity: 2,
          order_quantity: nil },
        # INCLUDE (order_quantity)
        { requested_quantity: 2,
          approved_quantity: 2,
          order_quantity: 1 }
      ].each do |qts|
        FactoryBot.create(
          :request,
          qts.merge(
            budget_period_id: bp.id,
            price_cents: 100
          )
        )
      end

      q = <<-GRAPHQL
        query {
          budget_periods(id: ["#{bp.id}"]) {
            id
            total_price_cents_inspected_requests
          }
        }
      GRAPHQL

      result = query(q, user.id)
      expect(
        result['data']['budget_periods'].first['total_price_cents_inspected_requests']
      ).to eq '400'
    end

    example 'authorizes total_price_cents_* query path' do
      FactoryBot.create(:request)

      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector, user_id: user.id)

      [:new, :inspected].each do |qty_type|
        tp = "total_price_cents_#{qty_type}_requests"

        q = <<-GRAPHQL
          query {
            budget_periods {
              id
              #{tp}
            }
          }
        GRAPHQL

        result = query(q, user.id)
        expect(result['data']['budget_periods'].first[tp]).to be_nil
        expect(result['errors'].first['message']).to match(/UnauthorizedException/)
      end
    end
  end

  context 'mutation' do
    before :example do
      budget_periods_before = [{ name: 'bp_to_delete' },
                               { name: 'bp_1' }]

      budget_periods_before.each do |data|
        FactoryBot.create(:budget_period, data)
      end

      budget_limits_before = [{ budget_period: { name: 'bp_to_delete' } },
                              { budget_period: { name: 'bp_1' } }]

      budget_limits_before.each do |data|
        FactoryBot.create(
          :budget_limit,
          budget_period_id: BudgetPeriod.find(data[:budget_period]).id
        )
      end

      now = DateTime.now
      @new_inspection_start_date_1 = DateTime.new(now.year + 1, 6, 1)
      @new_end_date_1 = DateTime.new(now.year + 1, 12, 1)
      @new_inspection_start_date_2 = DateTime.new(now.year + 2, 6, 1)
      @new_end_date_2 = DateTime.new(now.year + 2, 12, 1)

      @q = <<-GRAPHQL
        mutation {
          budget_periods (
            input_data: [
              { id: "#{BudgetPeriod.find(name: 'bp_1').id}",
                name: "bp_1_new_name",
                inspection_start_date: "#{@new_inspection_start_date_1}",
                end_date: "#{@new_end_date_1}" },
              { id: null,
                name: "new_bp",
                inspection_start_date: "#{@new_inspection_start_date_2}",
                end_date: "#{@new_end_date_2}" }
            ]
          ) {
            name
          }
        }
      GRAPHQL
    end

    #############################################################################

    it 'returns error for unauthorized user' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector, user_id: user.id)

      result = query(@q, user.id)

      expect(result['data']['budget_periods']).to be_blank
      expect(result['errors'].first['message']).to match(/UnauthorizedException/)

      expect(BudgetPeriod.all.map(&:name)).to be == ['bp_to_delete', 'bp_1']
    end


    it 'updates successfully for an authorized user' do
      user = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: user.id)

      result = query(@q, user.id)
      puts ">> user.id\n" + user.id
      puts ">> query\n" + @q
      puts ">> result =>\n" + result.to_json


      # sorted after `inspection_start_date DESC`
      expect(result).to eq({
        'data' => {
          'budget_periods' => [
            { 'name' => 'new_bp' },
            { 'name' => 'bp_1_new_name' }
          ]
        }
      })

      budget_periods_after = [
        { name: 'new_bp',
          inspection_start_date: @new_inspection_start_date_2,
          end_date: @new_end_date_2 },
        { name: 'bp_1_new_name',
          inspection_start_date: @new_inspection_start_date_1,
          end_date: @new_end_date_1 }
      ]


      # >>>{"data":{"budget_periods":[{"name":"new_bp"},{"name":"bp_1_new_name"}]}}
      # >>1>{"name":"new_bp","inspection_start_date":"2025-06-01T00:00:00.000+00:00","end_date":"2025-12-01T00:00:00.000+00:00"}
      # >>1>{"name":"bp_1_new_name","inspection_start_date":"2024-06-01T00:00:00.000+00:00","end_date":"2024-12-01T00:00:00.000+00:00"}
      #
      # >>2>{"budget_period":{"name":"bp_1_new_name"}}
      # >>3>{"name":"bp_1_new_name"}
      # >>4>c355012b-be8d-4905-ba4d-d634b3767448
      # updates successfully for an authorized user


      puts ">>>\n"+BudgetPeriod.to_json

      expect(BudgetPeriod.count).to be == budget_periods_after.count
      budget_periods_after.each do |data|
        puts ">>1>" + data.to_json

        expect(BudgetPeriod.find(data)).to be
      end

      budget_limits_after = [
        { budget_period: { name: 'bp_1_new_name' } }
      ]
      budget_limits_after.each do |data|
        puts ">>2>" + data.to_json
        puts ">>3>" + data[:budget_period].to_json
        puts ">>4>" + BudgetPeriod.find(data[:budget_period]).id

        FactoryBot.create(
          :budget_limit,
          budget_period_id: BudgetPeriod.find(data[:budget_period]).id
        )
      end
    end
  end
end
