require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool groups ', type: :feature do

  context ' an admin, a pool, and groups exist' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @groups = 10.times.map{ FactoryBot.create :group}
    end

    context 'an admin via the UI' do
      before(:each){ sign_in_as @admin }

      scenario ' manages roles of a groups' do
        @group = @groups.sample

        click_on 'Inventory Pools'
        click_on @pool.name
        within('.nav-tabs') { click_on 'Groups' }
        select "any", from: "Role"
        fill_in 'Search', with: @group.name
        wait_until { all("table tbody tr").count == 1 }
        expect(page.find("table")).not_to have_content "customer"
        expect(page.find("table")).not_to have_content "inventory_manager"
        click_on "Edit"
        wait_until{ not all(".modal").empty? }
        # set access_right
        check "inventory_manager"
        click_on "Save"
        wait_until do
          GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id])
            .try(:role) == "inventory_manager"
        end

        # remove all access_rights
        click_on "Edit"
        wait_until{ not all(".modal").empty? }
        uncheck :customer
        click_on "Save"
        wait_until do
          GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id]).nil?
        end

      end
    end
  end
end

