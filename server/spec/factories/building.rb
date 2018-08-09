class Building < Sequel::Model
end

FactoryBot.define do
  factory :building do
    name { Faker::Address.street_address }
  end

  trait :general do
    id 'abae04c5-d767-425e-acc2-7ce04df645d1' 
  end
end
