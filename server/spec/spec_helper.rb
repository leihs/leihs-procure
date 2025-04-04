require "pry"
require "active_support/all"

SERVER_DIR = Pathname.new(__dir__).join("..")
require SERVER_DIR.join("database/spec/config/database").to_s

require "config/factories"

def http_port
  @port ||= Integer(ENV["LEIHS_PROCURE_HTTP_PORT"].presence || 3230)
end

def http_host
  @host ||= ENV["LEIHS_PROCURE_HTTP_HOST"].presence || "localhost"
end

def http_base_url
  @http_base_url ||= "http://#{http_host}:#{http_port}"
end

RSpec.configure do |config|
  config.before(:example) do |example|
    srand 1
    db_clean
    db_restore_data seeds_sql
  end
end
