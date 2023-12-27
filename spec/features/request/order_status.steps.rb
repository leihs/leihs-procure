step "there is an inspector for category :cat" do |cat|
  c = ProcurementCategory.find(name: cat)
  pi = FactoryBot.create(:procurement_inspector, category: c)
  @inspector = pi.user
end

step "there is a viewer for category :cat" do |cat|
  c = ProcurementCategory.find(name: cat)
  pv = FactoryBot.create(:procurement_viewer, category: c)
  @viewer = pv.user
end

step ":prop filter has following checkboxes:" do |prop, table|
  within ".form-compact" do
    within find(".form-group", text: prop) do
      table.raw.flatten.each do |el|
        find("label", text: el, match: :first)
      end
    end
  end
end

step "I check/uncheck all items for :prop filter" do |prop|
  # Note: this only works for the combo filters with "Alle auswählen" button; but not for plain status checkboxes
  within ".form-compact" do
    within find(".form-group", text: prop) do
      find("button", match: :first).click
      find("label.dropdown-item", text: "Alle auswählen").click
      find("button", match: :first).click
    end
  end
end

step "I check/uncheck :opt for :prop filter" do |opt, prop|
  within ".form-compact" do
    within find(".form-group", text: prop) do
      if prop == "Status Antrag" || prop == "Status Beschaffung"
        find(".custom-checkbox label", text: /^#{opt}$/).click
      else
        find("button", match: :first).click
        find("label.dropdown-item", text: /^#{opt}$/).click
        find("button", match: :first).click
      end
    end
  end
end

step "I expand the line of the request for :article_name" do |article_name|
  @request = ProcurementRequest.find(article_name: article_name)
  step "I expand the request line"
end

step "I see a readonly :name field in the request form" do |name|
  within ".ui-request-form" do
    find_field(name, disabled: true)
  end
end

step "I don't see :content in the request form" do |content|
  expect(find(".ui-request-form")).not_to have_content(content)
end

step "I see :content in the request form" do |content|
  expect(find(".ui-request-form")).to have_content(content)
end
