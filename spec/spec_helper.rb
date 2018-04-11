ENV['LEIHS_DATABASE_URL'] = 'jdbc:postgresql://localhost:5432/leihs_test?max-pool-size=5'

require 'active_support/all'
require 'config/database'
require 'config/factories'
require 'pry'

RSpec.configure do |config|
end
