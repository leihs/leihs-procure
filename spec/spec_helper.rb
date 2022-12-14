require 'active_support/all'
require 'pry'

DIR = Pathname.new(__dir__)
require DIR.join('..').join('server/spec/config/database')

require 'config/database'
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

