require "capybara/rspec"

def take_screenshot(screenshot_dir = nil, name = nil)
  if !screenshot_dir.present?
    fail "no `screenshot_dir` given!" unless defined?(Rails)
    screenshot_dir = Rails.root.join("tmp", "capybara")
  end

  name ||= "screenshot_#{DateTime.now.utc.iso8601.tr(":", "-")}"
  name = "#{name}.png" unless name.ends_with?(".png")

  path = File.join(Dir.pwd, screenshot_dir, name)
  FileUtils.mkdir_p(File.dirname(path))

  case Capybara.current_driver
  when :firefox
    page.driver.browser.save_screenshot(path)
  else
    fail "Taking screenshots is not implemented for \
              #{Capybara.current_driver}."
  end
end
