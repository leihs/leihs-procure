require 'graphlient'

RSpec.shared_context 'graphql client' do
  let(:graphql_client) do
    Graphlient::Client.new('http://localhost:3211/procure/graphql')
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
