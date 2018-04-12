require 'graphlient'

RSpec.shared_context 'graphql client' do
  let(:graphql_url) do
    "#{ENV['LEIHS_HTTP_BASE_URL']}/procure/graphql"
  end
  let(:graphql_client) do
    Graphlient::Client.new(graphql_url)
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
