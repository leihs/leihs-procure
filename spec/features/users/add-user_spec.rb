require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an admin user and a bunch of users' do

    let :sign_in_as_admin do
      visit '/'
      click_on 'Sign in with password'
      fill_in 'email', with: @admin.email
      fill_in 'password', with: @admin.password
      click_on 'Sign in'
    end


    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      sign_in_as_admin
    end


    scenario 'adding a new user and signing in as the new user' do 

      visit '/admin/'
      click_on 'Users'
      click_on 'Add user'
      uncheck 'is_admin'
      check 'account_enabled'
      check 'password_sign_in_enabled'
      fill_in 'email', with: 'test@example.com'
      create_path = current_path
      click_on 'Create'
      # wait for redirect after the users has been created
      wait_until { current_path != create_path }

      click_on_first "Password" 
      fill_in 'password', with: 'password'
      password_path = current_path
      click_on 'Set'
      wait_until { current_path != password_path}

      # sign out and sign in as the new user
      visit '/'
      click_on 'Sign out'
      click_on 'Sign in with password'
      fill_in 'email', with: 'test@example.com' 
      fill_in 'password', with: 'password'
      click_on 'Sign in'
      expect(page).to have_content 'test@example.com'

    end

  end

end
