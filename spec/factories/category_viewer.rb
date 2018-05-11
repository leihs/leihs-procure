class CategoryViewer < Sequel::Model(:procurement_category_viewers)
end

FactoryBot.define do
  factory :category_viewer do
    user_id { create(:user).id }
    category_id { create(:category).id }
  end
end
