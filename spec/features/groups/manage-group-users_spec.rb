require 'spec_helper'
require 'pry'

feature 'Manage group users ', type: :feature do

  context 'an admin user and a bunch of other users and one group' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      @group = FactoryBot.create :group

      sign_in_as @admin
    end

    scenario 'adding and removing users' do 


      visit '/admin/'
      click_on 'Groups'
      click_on @group.name
      click_on 'Users'

      uncheck 'Group users only'

      click_on_first 'Add'
      wait_until do
        page.has_content? "1 User"
      end

      click_on_first 'Add'
      wait_until do
        page.has_content? "2 Users"
      end

      click_on_first 'Add'
      wait_until do
        page.has_content? "3 Users"
      end

      click_on_first 'Remove' 
      wait_until do
        page.has_content? "2 Users"
      end

    end

  end

end
