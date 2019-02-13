class LegacyAudit < Sequel::Model(:audits)
end

FactoryBot.define do
  factory :legacy_audit do
    comment { Faker::Lorem.sentences }
    created_at { Date.today - 1.year - 2.days }
  end
end

