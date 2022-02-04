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

step "the :name field is readonly" do |name|
  find_field(name, disabled: true)
end

step "the :name field is not readonly" do |name|
  find_field(name, disabled: false)
end
