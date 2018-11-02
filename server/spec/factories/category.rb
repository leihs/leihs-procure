class Category < Sequel::Model(:procurement_categories)
end

FactoryBot.define do
  factory :category do
    name { "#{Faker::Cat.name} #{Faker::Cat.breed}" }
    main_category_id { create(:main_category).id }
    general_ledger_account { Faker::Number.number(10) }
    cost_center { Faker::Number.number(10) }
    procurement_account { Faker::Number.number(10) }
  end
end
