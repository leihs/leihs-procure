# frozen_string_literal: true

feature 'smoke test' do
  before(:each) do
    @inspector = FactoryBot.create(:user)
    @main_category = FactoryBot.create(:procurement_main_category)
    @category = FactoryBot.create(:procurement_category, main_category: @main_category)
    FactoryBot.create(:procurement_inspector, user: @inspector, category: @category)
  end

  context 'as inspector' do
    before(:each) do
      Helpers::User.sign_in_as @inspector
      visit('/templates/edit')
    end

    scenario 'click on category' do
      first('h3[id^="mc"]').click
      find('button[type="submit"]').click
    end
  end
end
