# frozen_string_literal: true

feature 'smoke test' do
  before(:each) do
    @user = FactoryBot.create(:user)
    @main_category = FactoryBot.create(:procurement_main_category)
    @category = FactoryBot.create(:procurement_category, main_category: @main_category)
    FactoryBot.create(:procurement_inspector, user: @user, category: @category)

    visit('/templates/edit')
    fill_in('inputEmail', with: @user.email)
    find_button('Continue').click

    fill_in('inputPassword', with: 'password')
    find_button('Continue').click
  end

  context 'user is logged in' do
    scenario 'user clicks on category' do
      first('h3[id^="mc"]').click
      find('button[type="submit"]').click
    end
  end
end
