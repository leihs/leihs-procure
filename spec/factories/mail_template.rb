class MailTemplate < Sequel::Model
  # TEMPLATE_TYPES = {
  #   reminder: :user,
  #   deadline_soon_reminder: :user,
  #   received: :order,
  #   submitted: :order,
  #   approved: :order,
  #   rejected: :order
  # }
end

# FactoryBot.define do
#   factory :mail_template do
#     name { MailTemplate::TEMPLATE_TYPES.keys.sample }
#     is_template_template { true }
#     format { "text" }
#     language_locale { Language.where(active: true).sample.locale }

#     after(:build) do |templ|
#       templ.format = MailTemplate::TEMPLATE_TYPES[templ.name].to_s
#     end
#   end
# end
