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

      # suspend on the users table
      click_on 'Inventory-Pools'
      click_on @pool.name
      click_on "Users"
      select 'any', from: 'Role'
      fill_in 'Search', with: @user.email
      wait_until { all("table.users tbody tr").count == 1 }
      within("td.suspension"){ click_on 'Edit' }
      expect { page.not.to have_content "Suspended for" }
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until { page.has_content? "Suspended for" }

      # on the inventory-pool-user's page:
      # suspension info is here and we can cancel suspension
      click_on_first_user @user
      wait_until { page.has_content? "Suspended for" }
      wait_until { page.has_content? "Some reason" }
      within("#suspension"){ click_on "Edit" }
      wait_until{ not all(".modal").empty? }
      click_on "Reset suspension"
      click_on "Save"
      wait_until{ all(".modal").empty? }
      wait_until { page.has_content? "Not suspended" }
      # suspend again from here
      within("#suspension"){ click_on "Edit" }
      wait_until{ not all(".modal").empty? }
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until{ all(".modal").empty? }
      wait_until { page.has_content? "Suspended for" }


      # revoke suspension on users page
      click_on "Users"
      select 'any', from: 'Role'
      fill_in 'Search', with: @users.first.email
      wait_until { all("table.users tbody tr").count == 1 }
      within  "td.suspension" do
        expect { page.not_to have_content "Not suspended." }
        click_on "Edit"
      end
      wait_until{ not all(".modal").empty? }
      click_on "Reset suspension"
      click_on "Save"
      wait_until{ all(".modal").empty? }
      wait_until { page.has_content? "Not suspended" }

      # suspend forever and test suspension filter
      within("td.suspension"){ click_on "Edit" }
      wait_until{ not all(".modal").empty? }
      fill_in 'suspended_until', with: (Date.today + 100.years).iso8601
      click_on 'Save'
      wait_until{ all(".modal").empty? }
      select 'any', from: 'Role'
      select('suspended', from: 'Suspension')
      wait_until { all("table.users tbody tr").count == 1 }
      expect(page.find("table.users")).to have_content 'forever'

    end
  end
end
