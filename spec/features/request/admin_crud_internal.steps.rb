require "spec_helper"
require "pry"
require "net/http"
require "uri"

ICON_IMAGE_NAME = "icon-image.png"
MAIN_CATEGORY_FORM_MAPPING = {
  "name" => "Hauptkategorie",
  "categories.0.name" => "Subkategorien",
  "categories.0.cost_center" => "Kostenstelle",
  "categories.0.general_ledger_account" => "Sachkonto ⅠI (IR)",
  "categories.0.procurement_account" => "Sachkonto I (ER)"
}

step "I only see the close button" do
  within find("form[id='#{@request.id}']") do
    expect(current_scope.all("button").count).to eq 1
    current_scope.find("button", text: "Schliessen")
  end
end

step "I see the link for the request" do
  within find("form[id='#{@request.id}']") do
    current_scope.find("a", text: "Link zum Antrag")
  end
end

step "I open the AdminKategorien menu" do
  find("a", text: "Admin").click
  find("a", text: "Kategorien").click
end

def wait_for_link_or_throw(text, timeout = 10)
  # Prefer CSS + text over has_link?: under FF 153 / geckodriver 0.37,
  # Link Text find_elements can raise UnknownError ("Failed to interpret
  # value as array"), especially with a leftover confirm dialog open.
  wait_until(timeout) do
    page.has_css?("a.nav-link", text: text, visible: true, wait: 0)
  end
rescue Timeout::Error
  raise "Timeout: Failed to find the link with text '#{text}' within #{timeout} seconds."
end

step "I click category :main_category_name" do |main_category_name|
  wait_for_link_or_throw(main_category_name)

  link = find("a.nav-link", text: main_category_name, visible: true, wait: 3, match: :first)
  expect(link).not_to be nil
  link.click
  wait_for_procure_loading_gone
end

step "I see image-icon with correct response-status" do
  image_url = find("img.img-thumbnail")
  expect(image_url).not_to be nil

  image_url = find("img.img-thumbnail")[:src]
  uri = URI.parse(image_url)
  # Relative img src → resolve against the app under test.
  uri = URI.join(Capybara.app_host || LEIHS_PROCURE_HTTP_BASE_URL, image_url) if uri.relative?

  response = Net::HTTP.start(uri.host, uri.port,
    use_ssl: uri.scheme == "https",
    open_timeout: 5,
    read_timeout: 10) { |http| http.request(Net::HTTP::Get.new(uri)) }
  expect(response.code).to eq "200"
end

step "I see input-field for image-upload" do
  image_upload_field = find(".input-file-upload label")
  expect(image_upload_field).not_to be nil
  expect(image_upload_field.text).to eq "Neues Kategoriebild auswählen"
end

step "I see no image" do
  expect(page).to have_no_selector("img.img-thumbnail")
end

step "I click add main-category" do
  find("ul.nav:nth-child(2) > li:nth-child(2) > a:nth-child(1)").click
end

step "I click add category" do
  find(".card button#add_bp_btn_undefined").click
end

step "I enter the following data category form:" do |table|
  props = table.rows_hash
  if props["Bild"]
    if @request
      all('#budgetPeriods\.name > label > input[type=file]').each(&:click)
    end

    fp = File.absolute_path("spec/files/#{ICON_IMAGE_NAME}")
    find("input[type='file']", visible: false).attach_file(fp)

    find(".input-file-upload-list li", text: ICON_IMAGE_NAME).find(".fa-trash-can")
  end

  within find(".card-body") do
    current_scope.all("input").each do |input|
      key = MAIN_CATEGORY_FORM_MAPPING[input[:name]]
      if key.blank?
        next
      end

      input.set(props[key])
      expect(input.evaluate_script("this.validationMessage")).to eq ""
    end
  end

  # Category FormField uses StatefulInput; flush before Speichern.
  flush_stateful_input_debounce!
end
