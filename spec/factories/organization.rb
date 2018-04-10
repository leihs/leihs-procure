class Organization < Sequel::Model(:procurement_organizations)
end

FactoryBot.define do
  factory :organization do
    name { Faker::Commerce.department }
  end
end
