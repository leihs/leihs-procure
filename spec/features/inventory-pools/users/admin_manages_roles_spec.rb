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
      fill_in 'users-search-term', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).not_to have_content "customer"
      expect(page.find("table.users")).not_to have_content "inventory_manager"
      within_first("td.direct-roles", text: 'add'){ click_on 'add' }

      check "inventory_manager"
      click_on "Save"

      # check on user page
      find("i.fa-user").click
      within ".effective-roles" do
        ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end
      within ".direct-roles" do
        ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end


      # check on user page
      click_on "Users"
      # test filtering by role:
      select 'inventory_manager', from: 'Role'
      wait_until { all("table.users tbody tr").count == 1 }
      ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
        expect(page).to have_field(role, disabled: true, checked: true)
      end

      # now change the role to lending_manager
      click_on 'edit'
      uncheck 'inventory_manager'
      check 'lending_manager'
      click_on "Save"


      # check on user page
      find("i.fa-user").click
      within ".direct-roles" do
        ['customer', 'group_manager', 'lending_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end

      #check on users page
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      ['customer', 'group_manager', 'lending_manager'].each do |role|
        expect(page).to have_field(role, disabled: true, checked: true)
      end

      expect(find("table.users")).to have_content("customer")
      expect(find("table.users")).to have_content("group_manager")
      expect(find("table.users")).to have_content("lending_manager")
      expect(find("table.users")).not_to have_content("inventory_manager")


      # quick remove all roles
      within("table.users") { click_on 'remove' }
      wait_until do
        not find("table.users").has_content? "customer"
      end

    end
  end
end





