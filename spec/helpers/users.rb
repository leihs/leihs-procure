# frozen_string_literal: true

require_relative('../spec_helper')

module Helpers
  module User
    module_function

    def sign_in_as(user)
      Capybara.visit('/')
      Capybara.fill_in('inputEmail', with: user.email)
      Capybara.click_on('Continue')
      Capybara.fill_in('inputPassword', with: 'password')
      Capybara.click_on('Continue')
      # expect(page).to have_content user.lastname
      # visit '/admin/'
    end
  end
end
