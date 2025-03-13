RSpec.shared_context "request helper" do
  def transform_uuid_attrs(h)
    Request::UUID_ATTRS.each_with_object(h) do |attr, mem|
      if (value = mem[attr])
        attr_id = attr.to_s.concat("_id").to_sym
        mem[attr_id] = value
        mem.delete(attr)
      end
    end
  end
end

RSpec.configure do |config|
  config.include_context "request helper"
end
