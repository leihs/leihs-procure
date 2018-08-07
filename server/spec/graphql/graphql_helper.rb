require 'edn'
require 'faraday'

class GraphqlQuery
  URL = "#{Constants::LEIHS_HTTP_BASE_URL}/procure/graphql"
  CONN = Faraday.new(url: URL)

  attr_reader :response

  def initialize(query, user_id = nil, variables = nil)
    @query = query
    @variables = variables
    @user_id = user_id
  end

  def perform
    @response = CONN.post do |req|
      if @user_id 
        req.headers['X-Fake-Token-Authorization'] = @user_id
      end
      req.headers['Content-Type'] = 'application/json'
      req.body = { query: @query, variables: @variables }.to_json
    end
    self
  end

  def result
    JSON.parse @response.body
  end
end

RSpec.shared_context 'graphql client' do
  def query(q, user_id = nil, variables = {})
    GraphqlQuery.new(q, user_id, variables).perform.result
  end

  def hash_to_graphql(h)
    h.to_s.gsub(/:(.+?)=>/, "\\1: ")
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
