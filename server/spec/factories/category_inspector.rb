class CategoryInspector < Sequel::Model(:procurement_category_inspectors)
end

FactoryBot.define do
  factory :category_inspector do
    user_id { create(:user).id }
    category_id { create(:category).id }
  end
end
