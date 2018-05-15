require 'edn'
require 'faraday'

class GraphqlQuery
  URL = "#{Constants::LEIHS_HTTP_BASE_URL}/procure/graphql"
  CONN = Faraday.new(url: URL)

  def initialize(query, user_id = nil)
    @query = query
    @user_id = user_id
  end

  def perform
    @response = CONN.post do |req|
      if @user_id 
        req.headers['Authorization'] = @user_id
      end
      req.headers['Content-Type'] = 'application/json'
      req.body = { query: @query }.to_json
    end
    self
  end

  def result
    JSON.parse @response.body
  end
end

RSpec.shared_context 'graphql client' do
  def query(q, user_id = nil)
    GraphqlQuery.new(q, user_id).perform.result
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
