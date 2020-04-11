require 'spec_helper'
require 'pry'



feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, an inventory_manager, and  several users ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @inventory_manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @inventory_manager,
        inventory_pool: @pool, role: 'inventory_manager'
      @users = 10.times.map{ FactoryBot.create :user }
    end

    scenario ' managing roles of a user as an inventory_manager' do
      sign_in_as @inventory_manager

      visit "/admin/inventory-pools/#{@pool.id}"
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.lastname
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).not_to have_content "customer"
      expect(page.find("table.users")).not_to have_content "inventory_manager"
      click_on "none"
      @user_overview_page = current_path

      click_on_first "Direct Roles"
      check "customer"
      click_on "Save"
      wait_until { current_path == @user_overview_page }
      click_on_first "Direct Roles"
      check "inventory_manager"
      click_on "Save"

      click_on "Users"
      visit current_path # force full reload to make sure we not only see a fiction of the SPA
      # test filtering by role:
      select 'inventory_manager', from: 'Role'
      wait_until { all("table.users tbody tr").count == 2 }
      # the following also tests the current hierarchy within roles and will
      # break once we change that
      expect(page.find("table.users")).to have_content "customer"
      expect(page.find("table.users")).to have_content "group_manager"
      expect(page.find("table.users")).to have_content "lending_manager"
      expect(page.find("table.users")).to have_content "inventory_manager"


      # now remove all roles again
      visit @user_overview_page
      click_on_first "Direct Roles"
      expect(page).to have_field('customer', checked: true)
      expect(page).to have_field('group_manager', checked: true)
      expect(page).to have_field('lending_manager', checked: true)
      expect(page).to have_field('inventory_manager', checked: true)
      uncheck "customer"
      # this uses the hierarchy
      expect(page).to have_field('customer', checked: false)
      expect(page).to have_field('group_manager', checked: false)
      expect(page).to have_field('lending_manager', checked: false)
      expect(page).to have_field('inventory_manager', checked: false)

      click_on "Save"
      wait_until { current_path == @user_overview_page }
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.lastname
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).not_to have_content "customer"

    end
  end
end





