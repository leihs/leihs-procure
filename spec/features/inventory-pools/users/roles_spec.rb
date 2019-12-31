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

    scenario ' managing roles of a user' do

      visit "/admin/inventory-pools/#{@pool.id}"
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.lastname
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).not_to have_content "customer"
      expect(page.find("table.users")).not_to have_content "inventory_manager"
      click_on "none"
      @user_overview_page = current_path

      click_on "Direct Roles"
      check "customer"
      click_on "Save"
      wait_until { current_path == @user_overview_page }
      click_on "Manage Direct Roles"
      check "inventory_manager"
      click_on "Save"

      click_on "Users"
      visit current_path # force full reload to make sure we not only see a fiction of the SPA
      # test filtering by role:
      select 'inventory_manager', from: 'Role'
      wait_until { all("table.users tbody tr").count == 1 }
      # the following also tests the current hierarchy within roles and will
      # break once we change that
      expect(page.find("table.users")).to have_content "customer"
      expect(page.find("table.users")).to have_content "group_manager"
      expect(page.find("table.users")).to have_content "lending_manager"
      expect(page.find("table.users")).to have_content "inventory_manager"

    end
  end
end





