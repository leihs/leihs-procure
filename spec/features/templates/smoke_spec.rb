# frozen_string_literal: true

feature 'smoke test' do
  before(:each) do
    @user = FactoryBot.create(:user)
    @category = FactoryBot.create(:procurement_category)
    FactoryBot.create(:procurement_inspector, user: @user, category: @category)
  end

  scenario 'checks templates are available or throw errors' do
    visit('/templates/edit')
    binding.pry
    click_on(@category.name)
  end
end
