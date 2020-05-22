require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, and groups' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @groups = 10.times.map{ FactoryBot.create :group}
      sign_in_as @admin
    end

    scenario ' managing roles of a groups' do

      visit "/admin/inventory-pools/#{@pool.id}"
      click_on "Groups"
      fill_in 'groups-search-term', with: @groups.first.name
      wait_until { all("table.groups tbody tr").count == 1 }
      expect(page.find("table.groups ")).not_to have_content "customer"
      expect(page.find("table.groups ")).not_to have_content "inventory_manager"
      click_on "none"
      wait_until{ current_path.match? %r"/admin/inventory-pools/[^/]+/groups/[^/]+/roles" }
      _, _, _, inventory_pool_id, _, group_id, _  = current_path.split('/')

      # set access_right
      check "inventory_manager"
      click_on "Save"
      wait_until do
        GroupAccessRight.find(inventory_pool_id: inventory_pool_id, group_id: group_id)
          .try(:role) == "inventory_manager"
      end

      # remove all access_rights
      uncheck :customer
      click_on "Save"
      wait_until do
        GroupAccessRight.find(inventory_pool_id: inventory_pool_id, group_id: group_id).nil?
      end

    end
  end
end





