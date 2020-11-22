require 'spec_helper'
require 'pry'


feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, and  several users ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @users = 10.times.map{ FactoryBot.create :user }
      @user = @users.sample
      sign_in_as @admin
    end

    scenario ' managing the suspension of a user' do

      click_on 'Inventory-Pools'
      click_on @pool.name
      click_on "Users"
      select 'any', from: 'Role'
      fill_in 'Search', with: @user.email
      wait_until { all("table.users tbody tr").count == 1 }
      click_on 'Suspend'
      expect { page.not.to have_content "user is suspended" }
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until { page.has_content? "user is suspended" }


      # the inventory-pool-user's page:
      # suspension info is here and we can cancel suspension
      find('i.fa-user').click
      wait_until { page.has_content? "user is suspended" }
      wait_until { page.has_content? "Some reason" }
      within("#suspension") do
        expect(page).to have_content "Edit"
        click_on "Cancel"
      end
      wait_until { page.has_content? "Not suspended" }
      # suspend again from here
      click_on "Suspend"
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until { page.has_content? "user is suspended" }


      # revoke suspension on users page
      click_on "Users"
      select 'any', from: 'Role'
      fill_in 'Search', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      within  ".suspension" do
        expect { page.not_to have_content "unsuspended" }
        click_on "Cancel"
        wait_until { page.has_content? "unsuspended" }
      end

      # suspend forever and test suspension filter
      click_on "Suspend"
      fill_in 'suspended_until', with: (Date.today + 100.years).iso8601
      click_on 'Save'
      click_on 'Users'
      select 'any', from: 'Role'
      select('suspended', from: 'Suspension')
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).to have_content 'forever'

    end
  end
end
