require 'spec_helper'
require 'pry'

feature 'Manage inventory-pools', type: :feature do

  context 'an admin user' do

    before :each do
      @admin = FactoryBot.create :admin
      sign_in_as @admin
    end

    let(:name) { Faker::Company.name}
    let(:description) { Faker::Markdown.sandwich }
    let(:shortname) { Faker::Name.initials }
    let(:email) { Faker::Internet.email }

    scenario 'adding a new inventory-pool ' do

      visit '/admin/'
      click_on 'Inventory-Pools'
      click_on 'Add'
      fill_in 'name', with: name
      fill_in 'description', with: description
      fill_in 'shortname', with: shortname
      fill_in 'email', with: email
      check 'is_active'

      click_on 'Add'

      wait_until { current_path.match "^\/admin\/inventory-pools\/.+" }

      @inventory_pool_path = current_path
      @inventory_pool_id = current_path.match(/.*\/([^\/]+)/)[1]

      expect(page).to have_content name
      expect(page).to have_content shortname
      expect(page).to have_content email
      expect(page).to have_content description

      # The inventory pools path includes the newly created inventory pool and
      # we can get to it via clicking its name
      click_on "Inventory-Pools"
      wait_until { current_path == "/admin/inventory-pools/" }
      expect(page).to have_content name
      click_on name
      wait_until { current_path == @inventory_pool_path }

    end

  end

end
