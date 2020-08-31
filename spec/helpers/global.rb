module Helpers
  module Global
    extend self

    def wait_until(wait_time = 5, &block)
      Timeout.timeout(wait_time) do
        until value = yield
          sleep(0.2)
        end
        value
      end
    rescue Timeout::Error => e
      raise Timeout::Error.new(block.source)
    end

    def click_on_first(locator, options = {})
      wait_until(3) { first(:link_or_button, locator, options) }
      first(:link_or_button, locator, options).click
    end

    def within_first(locator, options = {}, &block)
      wait_until(3){first(locator, options)}
      within(first(locator, options)) do
        block.call
      end
    end


  end
end
