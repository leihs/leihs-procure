# test helpers
def wait_until(wait_time = 6, &block)
  Timeout.timeout(wait_time) do
    loop do
      begin
        value = yield
        break value if value
      rescue Selenium::WebDriver::Error::StaleElementReferenceError
        # RequestEdit swaps Loading ↔ form; held Capybara nodes go stale.
      end
      sleep(0.2)
    end
  end
rescue Timeout::Error
  raise Timeout::Error.new(block.source)
end

# StatefulInput debounces parent onChange by 100ms; Speichern reads React
# form state (not the DOM). Flush before submit under fast Firefox 153.
# 0.4s covers React setState + trailing debounce under FF 153.
STATEFUL_INPUT_DEBOUNCE_FLUSH_S = 0.4

def flush_stateful_input_debounce!
  sleep STATEFUL_INPUT_DEBOUNCE_FLUSH_S
end

def wait_for_procure_loading_gone(wait_time = 10)
  wait_until(wait_time) { page.has_no_css?(".fa-circle-notch", wait: 0) }
end

def request_edit_form_selector
  if @request
    "form[id='#{@request.id}']"
  else
    "form.ui-request-form"
  end
end

# Prefer page-scoped CSS over held element nodes: RequestEdit remounts the
# form when the edit query / mutation toggles Loading.
def wait_for_request_edit_form!(wait_time = 10)
  sel = request_edit_form_selector
  wait_until(wait_time) do
    page.has_css?("#{sel} .form-group", wait: 0) &&
      page.has_no_css?("#{sel} .fa-circle-notch", wait: 0)
  end
end

def request_save_context?
  # Only RequestEdit / RequestNew use form.ui-request-form. Speichern is also
  # used for AdminKategorien and Budgetperioden — those must not wait for a
  # request form (10s Timeout::Error each, and CI looks "stuck").
  @request || page.has_css?("form.ui-request-form", wait: 0)
end

def wait_after_request_save!(label, request_save: nil)
  return unless label.to_s.match?(/Speichern|Save/i)

  # Prefer the pre-click snapshot from the step; fall back for callers that
  # only pass the label.
  request_save = request_save_context? if request_save.nil?
  return wait_for_procure_loading_gone unless request_save

  if @request
    # Edit: mutate sets loading → form unmounts and <Loading/> shows while the
    # RequestLine is still open; onSuccess then closes the line. Waiting only
    # for the form to disappear returns during Loading, and the next "expand"
    # click toggles the line closed instead of open.
    wait_until(10) do
      page.has_no_css?("form[id='#{@request.id}']", wait: 0)
    end
    wait_for_procure_loading_gone
  else
    # Create: navigate to show page with flash + form.
    wait_until(10) do
      page.has_css?(".text-success", wait: 0) ||
        page.has_css?("form.ui-request-form", wait: 0)
    end
    wait_for_request_edit_form!
  end
end

# non-test helper methods
def backdoor(cmd)
  `vagrant ssh -- #{Shellwords.escape(cmd)}`
end
