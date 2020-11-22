class Contract < Sequel::Model(:contracts)
end

FactoryBot.define do
  factory :contract do
    user_id { User.all.sample.id }
    inventory_pool_id { InventoryPool.all.sample.id }
    compact_id { Faker::Alphanumeric.alpha(number: 10) }
    created_at { Time.now }
    updated_at { Time.now }
    state { "open" }
    purpose { Faker::Lorem.sentence }
    reservation_id { Reservation.all.sample.id }
  end
end
