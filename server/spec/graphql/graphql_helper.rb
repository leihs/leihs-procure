require 'edn'
require 'faraday'
require 'rack'

class GraphqlQuery
  attr_reader :response

  def initialize(query, user_id = nil, variables = nil)
    @query = query
    @variables = variables
    @user_id = user_id
    @csrf_token = get_csrf_token
    @cookies = get_cookies(user_id, @csrf_token)
  end

  def perform
    @response = Faraday.post("#{http_base_url}/procure/graphql") do |req|
      req.headers['Accept'] = 'application/json'
      req.headers['Content-Type'] = 'application/json'
      req.headers['X-CSRF-Token'] = @csrf_token
      req.body = { query: @query, variables: @variables }.to_json

      cookies = { "leihs-anti-csrf-token" => @csrf_token }

      if @cookies['leihs-user-session']
        cookies.merge!("leihs-user-session" => @cookies['leihs-user-session'])
      end

      req.headers['Cookie'] = cookies.map { |k, v| "#{k}=#{v}" }.join('; ')
    end

    self
  end

  def result
    JSON.parse @response.body
  end

  def get_csrf_token
    r = Faraday.get("#{http_base_url}/sign-in")
    r.body.match(/name="csrf-token".+value="(.*?)".*\/?>/)[1]
  end

  def get_cookies(user_id, csrf_token)
    resp = if user = User.find(id: user_id)
             Faraday.post("#{http_base_url}/sign-in",
                          { user: user.email, password: 'password' }) do |req|
               req.headers['X-CSRF-Token'] = csrf_token
               req.headers['Cookie'] = "leihs-anti-csrf-token=#{csrf_token}"
             end
           else
             Faraday.post(http_base_url)
           end

    Rack::Utils.parse_cookies_header(resp.headers['set-cookie'])
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
