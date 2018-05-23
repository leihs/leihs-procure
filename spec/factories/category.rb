class Category < Sequel::Model(:procurement_categories)
end

FactoryBot.define do
  factory :category do
    name { "#{Faker::Cat.name} #{Faker::Cat.breed}" }
    main_category_id { create(:main_category).id }
  end
end
