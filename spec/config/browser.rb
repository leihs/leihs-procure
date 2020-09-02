require 'capybara/rspec'
require 'selenium-webdriver'

BROWSER_DOWNLOAD_DIR= File.absolute_path(File.expand_path(__FILE__)  + "/../../../tmp")


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
      'browser.download.dir' => BROWSER_DOWNLOAD_DIR.to_s
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

  config.after(:each) do |example|
    unless example.exception.nil?
      take_screenshot screenshot_dir
    end
  end

  config.before :all do 
    FileUtils.remove_dir(screenshot_dir, force: true)
    FileUtils.mkdir_p(screenshot_dir)
  end

  def screenshot_dir
    Pathname(BROWSER_DOWNLOAD_DIR).join('screenshots')
  end

  def take_screenshot(screenshot_dir = nil, name = nil)
    name ||= "#{Time.now.iso8601.tr(':', '-')}.png"
    path = screenshot_dir.join(name)
    case Capybara.current_driver
    when :selenium
      page.driver.browser.save_screenshot(path) rescue nil
    when :poltergeist
      page.driver.render(path, full: true) rescue nil
    else
      Logger.warn "Taking screenshots is not implemented for \
              #{Capybara.current_driver}."
    end
  end
end
