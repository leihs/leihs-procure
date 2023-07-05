require 'active_support/all'
require 'pry'

PROJECT_DIR = Pathname.new(__dir__)
require PROJECT_DIR.join('..').join('server/database/spec/config/database')

require 'config/factories'
require 'config/metadata_extractor'
require 'config/screenshots'
require 'config/browser'
require 'config/rspec'

module Leihs
  module Constants
    GENERAL_BUILDING_UUID = 'abae04c5-d767-425e-acc2-7ce04df645d1'
  end
end


RSpec.configure do |config|
  config.before(:example) do |example|
    srand 1
    db_clean
    db_restore_data seeds_sql
  end
end
