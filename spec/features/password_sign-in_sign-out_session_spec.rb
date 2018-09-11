require 'spec_helper'

feature 'Passwords sign-in, sign-out, session' do

  context 'an existing admin user ' do

    before :each do
      @user = FactoryBot.create :admin, 
        email: 'admin@example.com', password: 'secret'
    end

    scenario 'signing in with the wrong password does not work' do

      visit '/'
      fill_in 'email', with: @user.email
      click_on 'Continue'
      fill_in 'password', with: 'bogus'
      click_on 'Sign in'

      wait_until do
        page.has_content? 'Password authentication failed!'
      end

    end

    scenario 'signing in with the correct password does work ' \
      'and we can see that we use session authentication ' do

      sign_in_as @user
      expect(page).to have_content @user.email

    end

    scenario 'signing out after signing in does work 'do

      sign_in_as @user
      click_on 'Sign out'

      # we are signed-out
      wait_until do
        not page.has_content? @user.email
      end

    end

  end
end
