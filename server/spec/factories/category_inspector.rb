class CategoryInspector < Sequel::Model(:procurement_category_inspectors)
  many_to_one :user
  many_to_one :category
end

FactoryBot.define do
  factory :category_inspector do
    user_id { create(:user).id }
    category_id { create(:category).id }
  end
end
