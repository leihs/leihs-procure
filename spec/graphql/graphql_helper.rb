require 'graphlient'

RSpec.shared_context 'graphql client' do
  let(:graphql_url) do
    "#{ENV['LEIHS_HTTP_BASE_URL']}/procure/graphql"
  end

  def graphql_client(auth_user_id = nil)
    headers = if auth_user_id
                { headers: { 'Authorization' => auth_user_id } }
              else
                {}
              end
    Graphlient::Client.new(graphql_url, headers)
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
