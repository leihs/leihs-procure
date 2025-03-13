step "I pry" do
  binding.pry # standard:disable Lint/Debugger
end

step "I debug :code" do |code|
  eval(code) # standard:disable Security/Eval
end

step "there is an empty database" do
  reset_database
end

step "I click on button :txt" do |txt|
  # FIXME: remove this sleep, after upgrading to vite/apollo this throws a api error
  sleep 0.1
  find_button(txt).click
end

step "I click on :txt" do |txt|
  sleep 0.1
  click_on(txt)
end

step "I click on :txt and accept the alert" do |txt|
  accept_alert { click_on txt }
end

step "I enter :value in the :name field" do |value, name|
  fill_in name, with: value
end

step "I go to :url" do |url|
  visit url
end

step "I visit :url" do |url|
  visit url
end

step "I am on :path" do |path|
  expect(page.current_path).to eq path
end

step "I am redirected to :url" do |url|
  binding.pry if url == "?" # standard:disable Lint/Debugger
  expect(page.current_path).to eq url
end

step "I see the text:" do |txt|
  expect(page).to have_content(txt.strip)
end

step "I see :txt" do |txt|
  expect(page).to have_content txt
end

step "I log in with the email :email" do |email|
  visit "/sign-in"
  within(".ui-form-signin") do
    step "I enter '#{email}' in the 'user' field"
    find('button[type="submit"]').click
  end
  within(".ui-form-signin") do
    step "I enter 'password' in the 'password' field"
    find('button[type="submit"]').click
  end
end

step "I take Screenshot" do
  page.execute_script('console.log("Opening the console.")')
  # Pause for a moment to allow time for the console to open
  sleep 1
  take_screenshot("tmp/error-screenshots")
end

step "I log in as the user" do
  step "I log in with the email '#{@user.email}'"
end

step "user's preferred language is :lang" do |lang|
  l = Language.find(name: lang)
  @user.update(language_id: l.id)
end

step "user does not have a prefered language" do
  expect(@user.language_id).to be_nil
end

step "I wait for :seconds seconds" do |seconds|
  sleep seconds.to_i
end
