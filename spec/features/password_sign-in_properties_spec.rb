require 'spec_helper'

feature 'Passwords sign-in, sign-out properties ' do
  context 'an existing admin user ' do

    before :each do
      @user = FactoryBot.create :admin
    end

    scenario 'changing the password as an admin does work' do

      visit '/'

      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      # set a new password in the user's edit page
      click_on @user.email
      click_on 'Edit'
      new_password = Faker::Internet.password(10, 20, true, true)

      fill_in 'password', with: new_password
      click_on 'Save'
      wait_until { not first('.modal')}

      # sign in with the new password does work
      click_on 'Sign out'
      visit '/'
      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: new_password
      click_on 'Sign in'
      expect(page).to have_content @user.email

    end

    scenario 'disabling sign_in_allowed immediatelly resets the session and prevents sign-in' do

      visit '/'

      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      # set a new password in the user's edit page
      click_on @user.email
      click_on 'Edit'
      uncheck 'sign_in_enabled'
      click_on 'Save'
      wait_until { first('.modal', text: 'OK') }

      sleep 1
      visit '/'

      # we are signed-out
      expect(page).not_to have_content @user.email

      # we can not sign in
      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).not_to have_content @user.email

    end

    scenario 'disabling password_sign_in_allowed prevents password sign-in' do

      visit '/'

      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      # set a new password in the user's edit page
      click_on @user.email
      click_on 'Edit'
      uncheck 'password_sign_in_enabled'
      click_on 'Save'
      wait_until { first('.modal', text: 'OK') }

      sleep 1

      visit '/'
      click_on 'Sign out'


      # we are signed-out
      expect(page).not_to have_content @user.email

      # we can not sign in
      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).not_to have_content @user.email

    end

  end
end

