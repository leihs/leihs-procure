require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an admin user and a bunch of users' do

    let :sign_in_as_admin do
      visit '/'
      click_on 'Sign in with password'
      fill_in 'email', with: @admin.email
      fill_in 'password', with: @admin.password
      click_on 'Sign in'
    end


    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      sign_in_as_admin
    end


    scenario 'deleting a user via delete ' do 

      @to_be_deleted_user = @users.first

      visit '/admin/'
      click_on 'Users'

      fill_in 'Search term', with: \
        "#{@to_be_deleted_user.firstname} #{@to_be_deleted_user.lastname}"

      click_on_first @to_be_deleted_user.lastname

      click_on 'Delete'

      click_on 'Delete'

      wait_until do
        page.has_content? "No (more) users found."
      end

      expect(database[:users].where(id: @to_be_deleted_user.id)).to be_empty

    end



    scenario 'deleting a user via transfere and delete' do 

      @to_be_deleted_user = @users.first

      visit '/admin/'
      click_on 'Users'

      fill_in 'Search term', with: \
        "#{@to_be_deleted_user.firstname} #{@to_be_deleted_user.lastname}"

      click_on_first @to_be_deleted_user.lastname

      click_on 'Delete'

      fill_in 'Target user id', with: @users.to_a.second.id

      click_on 'Transfer and delete'


      wait_until do
        page.has_content? "No (more) users found."
      end

      expect(database[:users].where(id: @to_be_deleted_user.id)).to be_empty

    end

  end

end
