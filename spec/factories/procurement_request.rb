require_relative './procurement_template'

class ProcurementRequest < Sequel::Model(:procurement_requests)
  many_to_one :template, class: ProcurementTemplate
  many_to_one :budget_period, class: ProcurementBudgetPeriod
  many_to_one :category, class: ProcurementCategory
  many_to_one :organization, class: ProcurementOrganization
  many_to_one :user
  many_to_one :room
end

FactoryBot.define do
  factory :procurement_request do
    user
    room
    association :budget_period, factory: :procurement_budget_period
    association :category, factory: :procurement_category
    association :organization, factory: :procurement_organization
    requested_quantity { 1 }
    motivation { Faker::Lorem.sentence }
    article_name { Faker::Commerce.product_name }

    trait :requested do
      association :template, factory: %i[procurement_template archiveable]
    end

    after :build do |r|
      r.organization = ProcurementRequester.find(user: r.user).organization
    end
  end
end
