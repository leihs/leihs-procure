require 'active_support/all'
require 'pry'

require 'config/database'
require 'config/factories'
require 'config/metadata_extractor'
require 'config/screenshots'

ACCEPTED_FIREFOX_ENV_PATHS = ['FIREFOX_ESR_60_PATH']

# switch to HTTPS ?
LEIHS_PROCURE_HTTP_BASE_URL = ENV['LEIHS_PROCURE_HTTP_BASE_URL'].presence || 'http://localhost:3230'
LEIHS_PROCURE_HTTP_PORT =  Addressable::URI.parse(LEIHS_PROCURE_HTTP_BASE_URL).port.presence  || '3230'

BROWSER_WINDOW_SIZE = [ 1200, 800 ]

Capybara.app_host = LEIHS_PROCURE_HTTP_BASE_URL

require 'config/rspec'
require 'config/browser'

module Leihs
  module Constants
    GENERAL_BUILDING_UUID = 'abae04c5-d767-425e-acc2-7ce04df645d1'
  end
end

