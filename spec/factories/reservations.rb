class Reservation < Sequel::Model(:reservations)
  many_to_one(:leihs_model, key: :model_id)
end

FactoryBot.define do
  factory :reservation do
    user_id { User.all.sample.id }
    inventory_pool_id { InventoryPool.all.sample.id }
    leihs_model
    status { "approved" }
    start_date { Date.tomorrow.to_s }
    end_date { (Date.tomorrow + 1.day).to_s }
    created_at { Time.now }
    updated_at { Time.now }
  end
end
