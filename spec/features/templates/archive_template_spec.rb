# frozen_string_literal: true

feature 'Archive Template(s)' do
  before(:each) do
    @user = FactoryBot.create(:user)
    @budget_period = FactoryBot.create(:procurement_budget_period, :inspection_phase)
    @category = FactoryBot.create(:procurement_category)
    @inspector = FactoryBot.create(:procurement_inspector, user: @user, category: @category)
    @requester = FactoryBot.create(:procurement_requester, user: @user)
    @templates = Array(1..5).map do |_|
      FactoryBot.create(:procurement_template, :archiveable, category: @category)
    end

    @requests = Array(1..5).map.with_index do |_element, index|
      template_object = @templates[index]
      FactoryBot.create(:procurement_request, category: @category, template_id: template_object.id, user: @user,
                                              budget_period: @budget_period)
    end

    visit('/templates/edit')
    fill_in('inputEmail', with: @user.email)
    # find(@user.email).send_keys(:enter)
    find_button('Continue').click

    fill_in('inputPassword', with: 'password')
    find_button('Continue').click
  end

  context 'user is logged in' do
    scenario 'user wants to archive single template' do
      archive_buttons = all('label[id^="btn_archive"]')
      archive_buttons.first.click
      find('button[type="submit"]').click
    end

    scenario 'user wants to darchive multiple templates' do
      archive_buttons = all('label[id^="btn_archive"]')
      archive_buttons.each(&:click)
      find('button[type="submit"]').click
    end
  end
end
