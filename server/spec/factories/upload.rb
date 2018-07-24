class Upload < Sequel::Model(:procurement_uploads)
end

FactoryBot.define do
  factory :upload, class: Upload do
    # TODO
  end
end
