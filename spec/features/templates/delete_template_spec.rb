# frozen_string_literal: true

feature 'Delete Template' do
  before(:each) do
    @inspector = FactoryBot.create(:user)
    @category = FactoryBot.create(:procurement_category)
    FactoryBot.create(:procurement_inspector, user: @inspector, category: @category)

    @templates = Array(1..5).map { |_| FactoryBot.create(:procurement_template, :deletable, category: @category) }
  end

  context 'as inspector' do
    before(:each) do
      Helpers::User.sign_in_as @inspector
      visit('/templates/edit')
    end

    scenario 'delete single template' do
      delete_buttons = all('label[id^="btn_del"]')
      delete_buttons.first.click
      find('button[type="submit"]').click
    end

    scenario 'delete multiple templates' do
      delete_buttons = all('label[id^="btn_del"]')
      delete_buttons.each(&:click)
      find('button[type="submit"]').click
    end
  end
end
