require 'active_support/all'
require 'config/database'
require 'config/factories'
require 'config/metadata_extractor'
require 'pry'



def http_port
  @port ||= Integer(ENV['LEIHS_PROCURE_HTTP_PORT'].presence || 3230)
end

def http_host
  @host ||= ENV['LEIHS_PROCURE_HTTP_HOST'].presence || 'localhost'
end

def http_base_url
  @http_base_url ||= "http://#{http_host}:#{http_port}"
end


RSpec.configure do |config|
end
