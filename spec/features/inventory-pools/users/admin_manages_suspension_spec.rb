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

    scenario ' managing the suspension of a user' do

      visit "/admin/inventory-pools/#{@pool.id}"
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }

      click_on 'suspend'
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
        expect(page).to have_content "edit"
        click_on "cancel"
      end
      wait_until { page.has_content? "Not suspended" }
      # suspend again from here
      click_on "suspend"
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until { page.has_content? "user is suspended" }


      # revoke suspension on users page
      click_on "Users"
      fill_in 'users-search-term', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      within  ".suspension" do
        expect { page.not_to have_content "unsuspended" }
        click_on "cancel"
        wait_until { page.has_content? "unsuspended" }
      end

      # suspend forever and test suspension filter
      click_on "suspend"
      fill_in 'suspended_until', with: (Date.today + 100.years).iso8601
      click_on 'Save'
      click_on 'Users'
      select('suspended', from: 'Suspension')
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).to have_content 'forever'

    end
  end
end
