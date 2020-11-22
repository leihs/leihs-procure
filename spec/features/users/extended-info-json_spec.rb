require 'spec_helper'
require 'pry'

feature 'Extended info of users ', type: :feature do

  context 'bunch of users exist, as an admin via the UI' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      sign_in_as @admin
    end


    scenario 'adding a new user with extended_info as json' do

      visit '/admin/'
      click_on 'Users'
      click_on 'Create user'
      uncheck 'is_admin'
      check 'account_enabled'
      check 'password_sign_in_enabled'
      fill_in 'email', with: 'test@example.com'
      fill_in 'extended_info', with: '{"foo": 42}'
      create_path = current_path
      click_on 'Create'
      wait_until do
        current_path.match "^\/admin\/users\/.+"
      end

      expect(page).to have_content "42"
    end

  end

end
