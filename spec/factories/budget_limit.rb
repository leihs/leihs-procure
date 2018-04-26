class BudgetLimit < Sequel::Model(:procurement_budget_limits)
end

FactoryBot.define do
  factory :budget_limit do
    main_category_id { create(:main_category).id }
    budget_period_id { create(:budget_period).id }
    amount_cents 10000
  end
end
