class Image < Sequel::Model(:procurement_images)
end

FactoryBot.define do
  factory :image, class: Image do
    transient do
      real_filename { "lisp-machine.jpg" }
    end

    filename { real_filename }
    content_type { "image/jpeg" }
    size { 160000 }
    main_category_id { create(:main_category).id }

    after(:build) do |image, evaluator|
      file_path = "spec/files/#{evaluator.real_filename}"
      file = File.new(file_path)

      image.content = Base64.encode64(file.read)
    end
  end
end
