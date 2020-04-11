require 'spec_helper'
require 'pry'


feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, and  several users ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @users = 10.times.map{ FactoryBot.create :user }
      sign_in_as @admin
    end

    scenario ' managing the suspension of a user' do

      visit "/admin/inventory-pools/#{@pool.id}"
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.lastname
      wait_until { all("table.users tbody tr").count == 1 }
      click_on "unsuspended"
      @user_overview_page = current_path

      click_on_first "Suspension"
      fill_in 'suspended_until', with: Date.today.iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until {current_path == @user_overview_page}
      expect(page).to have_content 'is suspended'

      # remove suspension
      click_on 'Remove Suspension'
      expect(page).to have_content 'Not suspended'


      click_on_first 'Suspension'
      fill_in 'suspended_until', with: (Date.today + 100.years).iso8601
      click_on 'Save'
      click_on 'Users'
      check "Suspended"
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).to have_content 'forever'

    end
  end
end
