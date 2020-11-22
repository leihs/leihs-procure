class Group < Sequel::Model(:groups)
end

FactoryBot.define do
  factory :group do
    name { Faker::Company.unique.name }
    description { Faker::Lorem.sentence }
    protected { rand < 0.5 }
  end
end
