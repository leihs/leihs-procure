class Reservation < Sequel::Model(:reservations)
end

FactoryBot.define do
  factory :reservation do
    user_id { User.all.sample.id }
    inventory_pool_id { InventoryPool.all.sample.id }
    status { "approved" }
    start_date { Date.tomorrow.to_s }
    end_date { (Date.tomorrow + 1.day).to_s }
    created_at { Time.now }
    updated_at { Time.now }
  end
end
