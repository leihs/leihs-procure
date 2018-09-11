require 'spec_helper'

feature 'Passwords sign-in, sign-out properties ' do
  context 'an existing admin user ' do

    before :each do
      @user = FactoryBot.create :admin
    end


    scenario 'disabling account_enabled immediatelly invalidates the session and prevents sign-in' do

      sign_in_as @user

      click_on "Users"
      within '.users' do
        click_on_first @user.lastname
      end

      click_on 'Edit'
      uncheck 'account_enabled'
      click_on 'Save'
      wait_until { first('.modal', text: 'OK') }

      sleep 1
      visit '/'

      # we are signed-out
      expect(page).not_to have_content @user.email

      fill_in 'email', with: @user.email
      click_on 'Continue'
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      wait_until do
        page.has_content? 'Password authentication failed!'
      end

    end

    scenario 'disabling password_sign_in_allowed prevents password sign-in' do

      sign_in_as @user

      click_on "Users"
      within '.users' do
        click_on_first @user.lastname
      end

      click_on 'Edit'
      uncheck 'password_sign_in_enabled'
      click_on 'Save'
      wait_until { first('.modal', text: 'OK') }

      sleep 1

      visit '/'
      click_on 'Sign out'


      # we are signed-out
      expect(page).not_to have_content @user.email

      fill_in 'email', with: @user.email
      click_on 'Continue'
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      wait_until do
        page.has_content? 'Password authentication failed!'
      end

    end
  end
end

