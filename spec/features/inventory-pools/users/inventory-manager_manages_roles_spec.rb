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
      select 'any', from: 'Role'
      fill_in 'Search', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).not_to have_content "customer"
      expect(page.find("table.users")).not_to have_content "inventory_manager"
      within_first("td.direct-roles", text: 'Add'){ click_on 'Add' }

      check "customer"
      click_on "Save"

      # check on user page
      find("i.fa-user").click
      within ".effective-roles" do
        expect(find_field('customer', disabled: true)).to be_checked
      end
      within ".direct-roles" do
        expect(find_field('customer', disabled: true)).to be_checked
      end

      # role to inventory_manager
      within(".direct-roles"){ click_on "Edit"}
      check "inventory_manager"
      click_on "Save"
      find("i.fa-user").click
      within ".direct-roles" do
        ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end

      click_on "Users"
      select 'any', from: 'Role'
      fill_in 'Search', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      expect(find("table.users")).to have_content "customer"
      # quick remove all roles
      within("table.users") { click_on 'Remove' }
      wait_until do
        not find("table.users").has_content? "customer"
      end

    end
  end
end
