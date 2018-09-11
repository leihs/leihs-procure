require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an admin user and a bunch of users' do

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


    scenario 'adding a new user ' do 

      visit '/admin/'
      click_on 'Users'
      click_on 'Add user'
      uncheck 'is_admin'
      check 'account_enabled'
      check 'password_sign_in_enabled'
      fill_in 'email', with: 'test@example.com'
      create_path = current_path
      click_on 'Create'
      wait_until do
        current_path.match "^\/admin\/users\/.+"
      end

    end

  end

end
