class Supplier < Sequel::Model
  one_to_many(:items)
end

FactoryBot.define do
  factory :supplier do
    name { Faker::Company.unique.name }
    note { Faker::Lorem.sentence }
  end
end
