class RequestCounter < Sequel::Model(:procurement_requests_counters)
end

FactoryBot.define do
  factory :request_counter do
    prefix
    counter
  end
end
