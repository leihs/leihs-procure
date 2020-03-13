require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an admin user and a bunch of users' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end

      @user = @users.sample

      sign_in_as @admin
    end


    context '@user has some inventory pool roles' do

      before :each do
        @inventory_pool1 = FactoryBot.create :inventory_pool
        FactoryBot.create :access_right, user: @user,
          inventory_pool: @inventory_pool1, role: 'customer'

        @inventory_pool2 = FactoryBot.create :inventory_pool
        FactoryBot.create :access_right, user: @user,
          inventory_pool: @inventory_pool2, role: 'inventory_manager'

      end

      scenario 'showing all inventory pool roles' do

        visit '/admin/'
        click_on 'Users'

        fill_in 'users-search-term', with: "#{@user.firstname} #{@user.lastname}"
        click_on "#{@user.firstname} #{@user.lastname}"

        click_on "2 inventory pool roles"

        wait_until { page.has_content? @inventory_pool1.name }
        expect(page).to have_content "customer"
        expect(page).to have_content @inventory_pool2.name
        expect(page).to have_content "inventory_manager"

      end
    end
  end
end

