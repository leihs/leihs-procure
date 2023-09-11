# frozen_string_literal: true

feature 'Delete Template' do
  before(:each) do
    @user = FactoryBot.create(:user)
    @category = FactoryBot.create(:procurement_category)
    @inspector = FactoryBot.create(:procurement_inspector, user: @user, category: @category)
    @templates = Array(1..5).map { |_| FactoryBot.create(:procurement_template, category: @category) }

    visit('/templates/edit')
    fill_in('inputEmail', with: @user.email)
    # find(@user.email).send_keys(:enter)
    find_button('Continue').click

    fill_in('inputPassword', with: 'password')
    find_button('Continue').click
  end

  context 'user is logged in' do
    scenario 'user wants to delete single template' do
      delete_buttons = all('label[id^="btn_del"]')
      delete_buttons.first.click
      find('button[type="submit"]').click
    end

    scenario 'user wants to delete multiple templates' do
      delete_buttons = all('label[id^="btn_del"]')
      delete_buttons.each(&:click)
      find('button[type="submit"]').click
    end
  end
end
