require 'capybara/rspec'
require 'selenium-webdriver'
require 'turnip/capybara'
require 'turnip/rspec'

BROWSER_WINDOW_SIZE = [ 1200, 800 ]
LEIHS_PROCURE_HTTP_PORT =  ENV['LEIHS_PROCURE_HTTP_PORT'].presence  || '3230'
LEIHS_PROCURE_HTTP_BASE_URL = ENV['LEIHS_PROCURE_HTTP_BASE_URL'].presence || "http://localhost:#{LEIHS_PROCURE_HTTP_PORT}"


BROWSER_DOWNLOAD_DIR= File.absolute_path(File.expand_path(__FILE__)  + "/../../../tmp")



firefox_bin_path = Pathname.new(`asdf where firefox`.strip).join('bin/firefox').expand_path.to_s
Selenium::WebDriver::Firefox.path = firefox_bin_path

Capybara.register_driver :firefox do |app|
  capabilities = Selenium::WebDriver::Remote::Capabilities.firefox(
    # TODO: trust the cert used in container and remove this:
    acceptInsecureCerts: true
  )

  profile = Selenium::WebDriver::Firefox::Profile.new
  # TODO: configure language for locale testing
  # profile["intl.accept_languages"] = "en"
  #
  profile_config = {
    'browser.helperApps.neverAsk.saveToDisk' => 'image/jpeg,application/pdf,application/json',
    'browser.download.folderList' => 2, # custom location
    'browser.download.dir' => BROWSER_DOWNLOAD_DIR.to_s
  }
  profile_config.each { |k, v| profile[k] = v }

  opts = Selenium::WebDriver::Firefox::Options.new(
    binary: firefox_bin_path,
    profile: profile,
    log_level: :trace)

  # NOTE: good for local dev
  if ENV['LEIHS_TEST_HEADLESS'].present?
    opts.args << '--headless'
  end
  # opts.args << '--devtools' # NOTE: useful for local debug

  # driver = Selenium::WebDriver.for :firefox, options: opts
  # Capybara::Selenium::Driver.new(app, browser: browser, options: opts)
  Capybara::Selenium::Driver.new(
    app,
    browser: :firefox,
    options: opts,
    desired_capabilities: capabilities
  )
end

Capybara.configure do |config|
  config.default_max_wait_time = 15
end
