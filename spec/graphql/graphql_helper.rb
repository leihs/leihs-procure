require 'graphlient'

RSpec.shared_context 'graphql client' do
  let(:graphql_client) do
    Graphlient::Client.new(ENV['LEIHS_HTTP_BASE_URL'])
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
