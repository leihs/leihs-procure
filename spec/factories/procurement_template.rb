# frozen_string_literal: true

class ProcurementTemplate < Sequel::Model(:procurement_templates)
  many_to_one :category, class: ProcurementCategory
end

FactoryBot.define do
  factory :procurement_template do
    article_name { Faker::Device.model_name }
    article_number { Faker::Device.model_name }
    price_cents { Faker::Commerce.price }
    price_currency { Faker::Currency.code }
    supplier_name { Faker::Camera.brand }
    is_archived { true }
    association :category, factory: :procurement_category
  end
end
