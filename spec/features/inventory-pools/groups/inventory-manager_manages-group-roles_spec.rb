require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, and groups' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @inventory_manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @inventory_manager,
        inventory_pool: @pool, role: 'inventory_manager'
      @groups = 10.times.map{ FactoryBot.create :group}
    end

    scenario ' managing roles of a groups as an inventory_manager' do
      sign_in_as @inventory_manager
      @group = @groups.first

      visit "/admin/inventory-pools/#{@pool.id}"
      within('.nav-tabs') { click_on 'Groups' }
      select 'any', from: 'Role'
      fill_in 'Search', with: @group.name
      wait_until { find("table").has_content?(@group.name) }
      expect(page.find("table ")).not_to have_content "customer"
      expect(page.find("table ")).not_to have_content "inventory_manager"

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

      expect(page).to have_css(
        '.modal .fade.alert.alert-danger', 
        text: "Users will be affected!"
      )
      # set access_right
      check "inventory_manager"
      click_on "Save"
      expect(GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id]).role).to eq "inventory_manager"

      # remove all access_rights
      within("table tbody tr", text: @group.name) do
        click_on "Edit"
      end
      wait_until{ not all(".modal").empty? }
      uncheck :customer
      click_on "Save"
      expect(GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id])).to be_nil

    end
  end
end





