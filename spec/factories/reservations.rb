class Reservation < Sequel::Model(:reservations)
end

FactoryBot.define do
  factory :reservation do
    user_id { User.all.sample.id }
    inventory_pool_id { InventoryPool.all.sample.id }
    status { "approved" }
    created_at { Time.now }
    updated_at { Time.now }
  end
end
