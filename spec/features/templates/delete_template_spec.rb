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

  context 'is logged in' do
    scenario 'insert data into form' do
      binding.pry
    end
  end
end
