class User < Sequel::Model
end

FactoryBot.define do
  factory :user do
    firstname { Faker::Name.first_name }
    lastname { Faker::Name.last_name }
    ############################
    # migrate to column defaults
    created_at { Date.today }
    updated_at { Date.today }
    ############################
  end
end
