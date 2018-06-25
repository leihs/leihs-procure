require 'edn'
require 'faraday'

class GraphqlQuery
  URL = "#{Constants::LEIHS_HTTP_BASE_URL}/procure/graphql"
  CONN = Faraday.new(url: URL)

  def initialize(query, user_id, variables)
    @query = query
    @variables = variables
    @user_id = user_id
  end

  def perform
    @response = CONN.post do |req|
      if @user_id 
        req.headers['Authorization'] = @user_id
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

  def map_type_to_gql(rb)
    case rb.class.name
    when "String"
      "\"#{rb}\""
    when "Float", "Integer", "Fixnum", "TrueClass", "FalseClass"
      rb
    else
      raise "unspecified ruby class"
    end
  end

  def hash_to_graphql(h)
    h
      .map { |k, v| "#{k}: #{map_type_to_gql v}" }
      .join(",\n")
      .insert(0, "{\n")
      .insert(-1, "\n}")
  end
end

RSpec.configure do |config|
  config.include_context 'graphql client'
end
