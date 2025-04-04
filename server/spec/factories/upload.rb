class Upload < Sequel::Model(:procurement_uploads)
end

FactoryBot.define do
  factory :upload, class: Upload do
    transient do
      real_filename { "secd.pdf" }
    end

    filename { real_filename }
    content_type { "application/pdf" }
    size { 56000 }

    after(:build) do |upload, evaluator|
      file_path = "spec/files/#{evaluator.real_filename}"
      file = File.new(file_path)

      upload.content = Base64.encode64(file.read)
    end
  end
end
