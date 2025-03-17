require "spec_helper"
require_relative "graphql_helper"

describe "price sums" do
  def first_category!
    @main_category_1 = FactoryBot.create(:main_category,
      name: "main_category_1")

    @category_1_a = FactoryBot.create(:category,
      main_category_id: @main_category_1.id,
      name: "category_1_a")
  end

  def requester
    user = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: user.id)
    user
  end

  def inspector
    user = FactoryBot.create(:user)
    FactoryBot.create(:category_inspector,
      category_id: @category_1_a.id,
      user_id: user.id)
    user
  end

  let :q do
    <<-GRAPHQL
      query RequestsIndexFiltered(
        $budgetPeriods: [ID]
        $categories: [ID]
        $priority: [Priority]
        $onlyOwnRequests: Boolean
      ) {
        dashboard(
          budget_period_id: $budgetPeriods,
          category_id: $categories,
          priority: $priority,
          requested_by_auth_user: $onlyOwnRequests
        ) {
          budget_periods {
            id
            total_price_cents
            main_categories {
              id
              total_price_cents
              categories {
                id
                total_price_cents
                requests {
                  id
                }
              }
            }
          }
        }
      }
    GRAPHQL
  end

  let :variables do
    {budgetPeriods: [@budget_period_i.id],
     categories: [@category_1_a.id,
       @category_1_b.id,
       @category_1_c.id,
       @category_2_a.id,
       @category_2_b.id,
       @category_2_c.id],
     priority: ["NORMAL"]}
  end

  def data!
    # ----------------------------------------------------------------------
    # first main category and category assumed already to be created

    # DENIED
    @request_i_1_a = FactoryBot.create(:request,
      category_id: @category_1_a.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      price_cents: 101,
      requested_quantity: 1,
      approved_quantity: 0)

    @category_1_b = FactoryBot.create(:category,
      main_category_id: @main_category_1.id,
      name: "category_1_b")
    # APPROVED
    @request_i_1_b = FactoryBot.create(:request,
      article_name: "Anaphoric Macro",
      category_id: @category_1_b.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      price_cents: 103,
      requested_quantity: 1,
      approved_quantity: 1,
      order_quantity: 0)
    # PARTIALLY APPROVED
    @request_ii_1_b = FactoryBot.create(:request,
      article_name: "Pandoric Macro",
      category_id: @category_1_b.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      price_cents: 107,
      requested_quantity: 2,
      approved_quantity: 1,
      order_quantity: 1)

    @category_1_c = FactoryBot.create(:category,
      main_category_id: @main_category_1.id,
      name: "category_1_c")

    @request_i_1_c = FactoryBot.create(:request,
      category_id: @category_1_c.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      price_cents: 109,
      requested_quantity: 1)

    # ----------------------------------------------------------------------
    #
    @main_category_2 = FactoryBot.create(:main_category,
      name: "main_category_2")

    @category_2_a = FactoryBot.create(:category,
      main_category_id: @main_category_2.id,
      name: "category_2_a")

    @request_i_2_a = FactoryBot.create(:request,
      category_id: @category_2_a.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      price_cents: 113,
      requested_quantity: 1,
      approved_quantity: 1,
      order_quantity: 1)

    @category_2_b = FactoryBot.create(:category,
      main_category_id: @main_category_2.id,
      name: "category_2_b")
    # priority 'high'
    @request_i_2_b = FactoryBot.create(:request,
      category_id: @category_2_b.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      priority: "high",
      price_cents: 127,
      requested_quantity: 1)

    @category_2_c = FactoryBot.create(:category,
      main_category_id: @main_category_2.id,
      name: "category_2_c")

    @category_2_d = FactoryBot.create(:category,
      main_category_id: @main_category_2.id,
      name: "category_2_d")

    # from a category not set in filter
    @request_i_2_d = FactoryBot.create(:request,
      category_id: @category_2_d.id,
      budget_period_id: @budget_period_i.id,
      user_id: @user.id,
      price_cents: 137,
      requested_quantity: 1)

    # =============================================================================

    @budget_period_ii = FactoryBot.create(:budget_period,
      name: "budget_period_ii")

    # from a budget period not set in filter
    @request_ii_1_a = FactoryBot.create(:request,
      category_id: @category_1_a.id,
      budget_period_id: @budget_period_ii.id,
      user_id: @user.id,
      price_cents: 139,
      requested_quantity: 1)
  end

  let :expected_result_transparent do
    {
      data: {
        dashboard: {
          budget_periods: [
            {id: @budget_period_i.id,
             total_price_cents: "329",
             main_categories: [
               {id: @main_category_1.id,
                total_price_cents: "216",
                categories: [
                  {id: @category_1_a.id,
                   total_price_cents: "0",
                   requests: [
                     {id: @request_i_1_a.id}
                   ]},
                  {id: @category_1_b.id,
                   total_price_cents: "107",
                   requests: [
                     {id: @request_i_1_b.id},
                     {id: @request_ii_1_b.id}
                   ]},
                  {id: @category_1_c.id,
                   total_price_cents: "109",
                   requests: [
                     {id: @request_i_1_c.id}
                   ]}
                ]},
               {id: @main_category_2.id,
                total_price_cents: "113",
                categories: [
                  {id: @category_2_a.id,
                   total_price_cents: "113",
                   requests: [
                     {id: @request_i_2_a.id}
                   ]},
                  {id: @category_2_b.id,
                   total_price_cents: "0",
                   requests: []},
                  {id: @category_2_c.id,
                   total_price_cents: "0",
                   requests: []},
                  {id: @category_2_d.id,
                   total_price_cents: "0",
                   requests: []}
                ]}
             ]}
          ]
        }
      }
    }
  end

  before :example do
    first_category!
  end

  context "requester" do
    context "budget period not past" do
      let :expected_result do
        {
          data: {
            dashboard: {
              budget_periods: [
                {id: @budget_period_i.id,
                 total_price_cents: "640",
                 main_categories: [
                   {id: @main_category_1.id,
                    total_price_cents: "527",
                    categories: [
                      {id: @category_1_a.id,
                       total_price_cents: "101",
                       requests: [
                         {id: @request_i_1_a.id}
                       ]},
                      {id: @category_1_b.id,
                       total_price_cents: "317",
                       requests: [
                         {id: @request_i_1_b.id},
                         {id: @request_ii_1_b.id}
                       ]},
                      {id: @category_1_c.id,
                       total_price_cents: "109",
                       requests: [
                         {id: @request_i_1_c.id}
                       ]}
                    ]},
                   {id: @main_category_2.id,
                    total_price_cents: "113",
                    categories: [
                      {id: @category_2_a.id,
                       total_price_cents: "113",
                       requests: [
                         {id: @request_i_2_a.id}
                       ]},
                      {id: @category_2_b.id,
                       total_price_cents: "0",
                       requests: []},
                      {id: @category_2_c.id,
                       total_price_cents: "0",
                       requests: []},
                      {id: @category_2_d.id,
                       total_price_cents: "0",
                       requests: []}
                    ]}
                 ]}
              ]
            }
          }
        }
      end

      it "requesting phase" do
        @budget_period_i = FactoryBot.create(:budget_period,
          :requesting_phase,
          name: "budget_period_i")
        @user = requester
        data!
        result = query(q, @user.id, variables).deep_symbolize_keys
        expect(result).to eq(expected_result)
      end

      it "inspection phase" do
        @budget_period_i = FactoryBot.create(:budget_period,
          :inspection_phase,
          name: "budget_period_i")
        @user = requester
        data!
        result = query(q, @user.id, variables).deep_symbolize_keys
        expect(result).to eq(expected_result)
      end
    end

    it "budget period past" do
      @budget_period_i = FactoryBot.create(:budget_period,
        :past,
        name: "budget_period_i")
      @user = requester
      data!
      result = query(q, @user.id, variables).deep_symbolize_keys
      expect(result).to eq(expected_result_transparent)
    end
  end

  context "inspector" do
    it "budget period past" do
      @budget_period_i = FactoryBot.create(:budget_period,
        :past,
        name: "budget_period_i")
    end

    context "budget period is not past" do
      it "requesting phase" do
        @budget_period_i = FactoryBot.create(:budget_period,
          :requesting_phase,
          name: "budget_period_i")
      end

      it "inspection phase" do
        @budget_period_i = FactoryBot.create(:budget_period,
          :inspection_phase,
          name: "budget_period_i")
      end
    end

    after :example do
      @user = inspector
      data!
      result = query(q, @user.id, variables).deep_symbolize_keys
      expect(result).to eq(expected_result_transparent)
    end
  end
end
