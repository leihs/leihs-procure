require 'pry'
require 'capybara/rspec'
require 'selenium-webdriver'
require 'faraday'
require 'faraday_middleware'

BROWSER_DONWLOAD_DIR= File.absolute_path(File.expand_path(__FILE__)  + "/../../../tmp")

def base_url
  @base_url ||= ENV['LEIHS_ADMIN_HTTP_BASE_URL'].presence || 'http://localhost:3220'
end

def port
  @port ||= Addressable::URI.parse(base_url).port
end

def plain_faraday_json_client
  @plain_faraday_json_client ||= Faraday.new(
    url: base_url,
    headers: { accept: 'application/json' }) do |conn|
      conn.adapter Faraday.default_adapter
      conn.response :json, content_type: /\bjson$/
    end
end

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


def plain_faraday_client
  Faraday.new(
    url: base_url,
    headers: { accept: 'application/json' }) do |conn|
      conn.adapter Faraday.default_adapter
      conn.response :json, content_type: /\bjson$/
    end
end
