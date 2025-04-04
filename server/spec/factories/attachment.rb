class Attachment < Sequel::Model(:procurement_attachments)
end

FactoryBot.define do
  f_name = "secd.pdf"
  f_path = "spec/files/#{f_name}"

  factory :attachment, class: Attachment do
    filename { f_name }
    content_type { "application/pdf" }
    size { 56883 }
    content { Base64.encode64(File.new(f_path).read) }
    request_id { create(:request).id }
  end
end
