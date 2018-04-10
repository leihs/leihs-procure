class Category < Sequel::Model(:procurement_categories)
end

FactoryBot.define do
  factory :category do
    name { Faker::Cat.name }
    main_category_id { create(:main_category).id }
  end
end
