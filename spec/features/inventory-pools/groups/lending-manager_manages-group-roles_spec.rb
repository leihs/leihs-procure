require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool roles of groups', type: :feature do

  context 'an admin, a pool, groups and a lending_manager exist' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager,
        inventory_pool: @pool, role: 'lending_manager'
      @groups = 10.times.map{ FactoryBot.create :group}
    end


    context "the lending_manager via the UI" do
      before(:each) { sign_in_as @manager }

      context "visits the roles page of a group" do
        before :each do
          @group = @groups.first
          @group_id = @group[:id]
          @inventory_pool_id = @pool[:id]
          click_on "Inventory Pools"
          click_on @pool.name
          within('.nav-tabs') { click_on "Groups" }
          select 'any', from: 'Role'
          fill_in 'Search', with: @groups.first.name
          wait_until { find("table").has_content?(@group.name) }
          expect(page.find("table")).not_to have_content "customer"
          expect(page.find("table")).not_to have_content "inventory_manager"
          
          #####################################################################
          # NOTE: Capybara finds the button and clicks it but nothing happens.
          # It has to be retried some times until the modal is displayed.
          # Some UI hooks/callbacks not initialized yet on first try?
          wait_until do
            within find("table tbody tr", text: @group.name) do
              find("button", text: "Edit").click
            end
            page.has_selector?(".modal")
          end
          #####################################################################
        end

        scenario 'can manage roles up to lending_manager' do

          # set access_right
          check "lending_manager"
          click_on "Save"
          wait_until do
            GroupAccessRight.find(inventory_pool_id: @inventory_pool_id, group_id: @group_id)
              .try(:role) == "lending_manager"
          end

          # remove all access_rights
          #####################################################################
          # NOTE: Capybara finds the button and clicks it but nothing happens.
          # It has to be retried some times until the modal is displayed.
          # Some UI hooks/callbacks not initialized yet on first try?
          wait_until do
            within find("table tbody tr", text: @group.name) do
              find("button", text: "Edit").click
            end
            page.has_selector?(".modal")
          end
          #####################################################################

          uncheck :customer
          click_on "Save"
          wait_until do
            GroupAccessRight.find(inventory_pool_id: @inventory_pool_id, group_id: @group_id).nil?
          end

        end

        scenario "can not escalate roles up to inventory_manager" do
          # try to set inventory_manager ar
          # wait_until do
          #   within find("table tbody tr", text: @group.name) do
          #     find("button", text: "Edit").click
          #   end
          #   page.has_selector?(".modal")
          # end
          check "inventory_manager"
          click_on "Save"
          wait_until { page.has_content? "ERROR 403" }
          wait_until do
            GroupAccessRight.find(inventory_pool_id: @inventory_pool_id, group_id: @group_id).nil?
          end

        end

      end

    end

  end

end
