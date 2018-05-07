require 'active_support/all'
require 'config/database'
require 'config/factories'
require 'pry'

# Require factories as they contain model class definitions.
# This is needed for models/* tests.
# Dir["factories/*.rb"].each { |file| require file }

RSpec.configure do |config|
end
