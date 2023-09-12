# frozen_string_literal: true

feature 'Unarchive Template(s)' do
  before(:each) do
    @user = FactoryBot.create(:user)
    @category = FactoryBot.create(:procurement_category)
    @budget_period = FactoryBot.create(:budget_period, :inspection_phase)
    @inspector = FactoryBot.create(:procurement_inspector, user: @user, category: @category)
    @templates = Array(1..5).map do |_|
      FactoryBot.create(:procurement_template, :unarchiveable, category: @category, budget_period: @budget_period)
    end

    visit('/templates/edit')
    fill_in('inputEmail', with: @user.email)
    # find(@user.email).send_keys(:enter)
    find_button('Continue').click

    fill_in('inputPassword', with: 'password')
    find_button('Continue').click
  end

  context 'user is logged in' do
    scenario 'user wants to delete single template' do
      show_archived_buttons = all('label[for^="archiveSwitch"]', visible: :all)
      # unhide archived templates
      show_archived_buttons.first.click
      binding.pry
      archive_buttons = all('label[id^="btn_archive"]')
      binding.pry
      find('button[type="submit"]').click
      binding.pry
    end

    # scenario 'user wants to delete multiple templates' do
    #   delete_buttons = all('label[id^="btn_del"]')
    #   delete_buttons.each(&:click)
    #   find('button[type="submit"]').click
    # end
  end
end
