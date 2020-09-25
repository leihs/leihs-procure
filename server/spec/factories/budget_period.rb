class BudgetPeriod < Sequel::Model(:procurement_budget_periods)
end

FactoryBot.define do
  factory :budget_period do
    inspection_start_date { DateTime.now + 30 }
    end_date { DateTime.now + 90 }
    name { "#{Faker::Cat.name} #{Faker::Cat.breed}".gsub(" ", "-")  }
  end

  trait :requesting_phase do
  end

  trait :inspection_phase do
    inspection_start_date { DateTime.now - 1.day }
    end_date { DateTime.now + 90 }
  end

  trait :past do
    inspection_start_date { DateTime.now - 1.week }
    end_date { DateTime.now - 1.day }
  end
end
