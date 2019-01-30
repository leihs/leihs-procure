require 'spec_helper'
require 'pry'


feature 'System-Admins', type: :feature do

  context 'system_admin and a user' do

    before :each do
      @system_admin =  FactoryBot.create :system_admin
      @user = FactoryBot.create :user

      sign_in_as @system_admin
    end

    scenario 'adding and removing a direct system_admin' do 
      click_on_first 'System'
      click_on 'System-Admins'
      expect(find("table.users").text).not_to include @user.email
      click_on 'Direct-Users'
      fill_in 'users-search-term', with: @user.lastname
      wait_until {all('table.users tbody tr').count == 1 }
      click_on 'Add'
      click_on 'System-Admins'
      wait_until do
        find("table.users").text.include? @user.email
      end
      click_on 'Direct-Users'
      fill_in 'users-search-term', with: @user.lastname
      wait_until {all('table.users tbody tr').count == 1 }
      click_on 'Remove'
      click_on 'System-Admins'
      wait_until do
        not (find("table.users").text.include? @user.email)
      end
    end

  end

end
