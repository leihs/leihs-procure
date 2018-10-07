
require 'spec_helper'
require 'pry'

feature 'Authentication-Systems', type: :feature do

  context 'a system_admin and a normal user' do

    before :each do
      @system_admin =  FactoryBot.create :system_admin, 
        email: 'admin@example.com', password: 'secret'

      @simple_user=  FactoryBot.create :user
    end

    scenario 'CRUD' do

      sign_in_as @system_admin

      click_on 'Authentication-Systems'

      click_on 'Add authentication-system'

      fill_in 'id', with: 'test-auth-system'
      fill_in 'name', with: 'Test Authentication-System'
      fill_in 'type', with: 'external'
      fill_in 'description', with: 'foo bar baz'
      fill_in 'internal_private_key', with: 'INT PRIV-KEY'
      fill_in 'internal_public_key', with: 'INT PUB-KEY'
      fill_in 'external_public_key', with: 'EXT PUB-KEY'
      click_on 'Add'
      wait_until do
        current_path.match /authentication-systems\/test-auth-system$/
      end
      expect(page).to have_content 'foo bar baz'

      click_on 'Edit'
      fill_in 'description', with: 'baz bar foo'
      click_on 'Save'
      wait_until do
        current_path.match /authentication-systems\/test-auth-system$/
      end
      expect(page).to have_content 'baz bar foo'

      click_on 'Delete'
      wait_until { current_path.match /delete$/ }
      click_on 'Delete'
      wait_until { current_path.match /authentication-systems\/$/ }

    end

  end

end

