require "capybara/rspec"
require "selenium-webdriver"
require "turnip/capybara"
require "turnip/rspec"

BROWSER_WINDOW_SIZE = [1200, 800]
LEIHS_PROCURE_HTTP_PORT = ENV["LEIHS_PROCURE_HTTP_PORT"].presence || "3230"
LEIHS_PROCURE_HTTP_BASE_URL = ENV["LEIHS_PROCURE_HTTP_BASE_URL"].presence || "http://localhost:#{LEIHS_PROCURE_HTTP_PORT}"

BROWSER_DOWNLOAD_DIR = File.absolute_path(File.expand_path(__FILE__) + "/../../../tmp")

# Mirrors bin/env/select-tool-versions-manager: TOOL_VERSIONS_MANAGER wins,
# otherwise mise if available, else asdf. The bin/env/*-setup scripts export
# the variable only within their own process, so rspec cannot rely on it
# (on a mise-only executor this shelled out to `asdf where firefox`).
tool_versions_manager = ENV["TOOL_VERSIONS_MANAGER"].to_s.strip
if tool_versions_manager.empty?
  tool_versions_manager = system("type mise > /dev/null 2>&1") ? "mise" : "asdf"
end
firefox_bin_path = if tool_versions_manager == "mise"
  Pathname.new(`mise where firefox`.strip).join("bin/firefox").expand_path.to_s
else
  Pathname.new(`asdf where firefox`.strip).join("bin/firefox").expand_path.to_s
end
# Only pin the binary when it exists: rspec dry runs never start a browser
# and may run before firefox-setup installed the version from .tool-versions.
Selenium::WebDriver::Firefox.path = firefox_bin_path if File.file?(firefox_bin_path)

Capybara.register_driver :firefox do |app|
  profile = Selenium::WebDriver::Firefox::Profile.new
  profile["intl.accept_languages"] = "de"
  profile_config = {
    "browser.helperApps.neverAsk.saveToDisk" => "image/jpeg,application/pdf,application/json",
    "browser.download.folderList" => 2, # custom location
    "browser.download.dir" => BROWSER_DOWNLOAD_DIR.to_s
  }
  profile_config.each { |k, v| profile[k] = v }

  opts = Selenium::WebDriver::Firefox::Options.new(
    binary: firefox_bin_path,
    profile: profile,
    log_level: :trace,
    # TODO: trust the cert used in container and remove this:
    accept_insecure_certs: true
  )

  # NOTE: good for local dev
  if ENV["LEIHS_TEST_HEADLESS"].present?
    opts.args << "--headless"
  end
  # opts.args << '--devtools' # NOTE: useful for local debug

  # On CI many feature trials start Firefox at the same moment; Selenium's
  # default port probing (first free port above 4444) then hands two
  # geckodrivers the same port and one session ends up talking to the other
  # trial's driver until that one exits (ECONNREFUSED mid-test). CIDER-CI
  # assigns each trial a dedicated port via `ports:` in the job config.
  driver_opts = {browser: :firefox, options: opts}
  if ENV["LEIHS_PROCURE_GECKODRIVER_PORT"].present?
    driver_opts[:service] = Selenium::WebDriver::Service.firefox(
      port: Integer(ENV["LEIHS_PROCURE_GECKODRIVER_PORT"])
    )
  end
  Capybara::Selenium::Driver.new(app, **driver_opts)
end

Capybara.configure do |config|
  Capybara.app_host = LEIHS_PROCURE_HTTP_BASE_URL
  Capybara.server_port = LEIHS_PROCURE_HTTP_PORT
  Capybara.default_driver = :firefox
  Capybara.current_driver = :firefox

  config.default_max_wait_time = 15
end
