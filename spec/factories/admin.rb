class Admin < Sequel::Model(:procurement_admins)
end

FactoryBot.define do
  factory :admin do
    user_id { FactoryGirl.create(:user).id }
  end
end
