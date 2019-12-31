require 'spec_helper'
require 'pry'


feature 'Manage inventory-pools', type: :feature do

  context ' an admin and several pools ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pools = 10.times.map { FactoryBot.create :inventory_pool }
      sign_in_as @admin
    end

    scenario 'edit an inventory pool' do

      visit '/admin/'
      click_on 'Inventory-Pools'

      @pools.each { |pool| expect(page).to have_content pool.name }

      click_on @pools.first.name
      @inventory_pool_path = current_path

      click_on 'Edit'

      fill_in 'name', with: 'The New Name'
      fill_in 'description', with: 'Foo Bar Baz'
      fill_in 'shortname', with: 'TNN'
      fill_in 'email', with: 'new-name@example.com'
      uncheck 'is_active'

      click_on 'Save'

      wait_until {current_path == @inventory_pool_path}

      expect(page).to have_content 'The New Name'
      expect(page).to have_content 'Foo Bar Baz'
      expect(page).to have_content 'TNN'
      expect(page).to have_content 'new-name@example.com'

      click_on 'Inventory-Pools'
      wait_until { current_path ==  "/admin/inventory-pools/" }
      expect(page).to have_content 'The New Name'

    end

  end

end

