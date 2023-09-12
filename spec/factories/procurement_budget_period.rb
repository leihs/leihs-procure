class ProcurementBudgetPeriod < Sequel::Model
end

FactoryBot.define do
  sequence :name do |n|
    "test#{n}"
  end
  factory :procurement_budget_period do
    name { generate(:name) }
    inspection_start_date { Date.yesterday }
    end_date { Date.today + 1.months }

    trait :requesting_phase do
      name { 'BP-in-requesting-phase' }
      inspection_start_date { Date.today + 2.month }
      end_date { Date.today + 3.months }
    end

    trait :past_phase do
      name { 'BP-in-past-phase' }
      inspection_start_date { Date.today - 3.months }
      end_date { Date.yesterday }
    end
  end
end
