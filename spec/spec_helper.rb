require 'config/database'
require 'graphlient'
require 'pry'
require 'sequel'

RSpec.configure do |config|
  config.include Config::Database
end
