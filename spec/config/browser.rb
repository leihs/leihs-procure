require 'capybara/rspec'
require 'selenium-webdriver'

BROWSER_DONWLOAD_DIR= File.absolute_path(File.expand_path(__FILE__)  + "/../../../tmp")


def set_capybara_values
  Capybara.app_host = base_url
  Capybara.server_port = port
end

def set_browser(example)
  Capybara.current_driver = \
    begin
      ENV['CAPYBARA_DRIVER'].presence.try(:to_sym) \
          || example.metadata[:driver] \
          || :selenium
    rescue
      :selenium
    end
end

ACCEPTED_FIREFOX_ENV_PATHS = ['FIREFOX_ESR_45_PATH']

def accepted_firefox_path 
  ENV[ ACCEPTED_FIREFOX_ENV_PATHS.detect do |env_path|
    ENV[env_path].present?
  end || ""].tap { |path|
    path.presence or raise "no accepted FIREFOX found"
  }
end

RSpec.configure do |config|
  set_capybara_values

  Selenium::WebDriver::Firefox.path = accepted_firefox_path

  Capybara.register_driver :selenium do |app|

    profile_config = {
      'browser.helperApps.neverAsk.saveToDisk' => 'image/jpeg,application/pdf,application/json',
      'browser.download.folderList' => 2, # custom location
      'browser.download.dir' => BROWSER_DONWLOAD_DIR.to_s
    }

    profile = Selenium::WebDriver::Firefox::Profile.new
    profile_config.each { |k, v| profile[k] = v }

    client = Selenium::WebDriver::Remote::Http::Default.new

    Capybara::Selenium::Driver.new \
      app, browser: :firefox, profile: profile, http_client: client

  end

  Capybara.current_driver = :selenium

  config.before :all do
    set_capybara_values
  end

  config.before :each do |example|
    set_capybara_values
    set_browser example
  end
end



