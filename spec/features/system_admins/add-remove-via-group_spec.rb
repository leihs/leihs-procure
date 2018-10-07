require 'spec_helper'
require 'pry'


feature 'System-Admins', type: :feature do

  context 'system_admin, a user in a system_admin_group' do

    before :each do
      @system_admin =  FactoryBot.create :system_admin
      @user = FactoryBot.create :user

      @system_admin_group = FactoryBot.create :group
      database[:groups_users].insert user_id: @user.id, group_id: @system_admin_group.id

      sign_in_as @system_admin
    end

    scenario 'adding and removing a system admin-group' do 
      click_on 'System-Admins'
      expect(find("table.users").text).not_to include @user.email
      click_on 'Groups'
      click_on 'Add'
      click_on 'System-Admins'
      wait_until do
        find("table.users").text.include? @user.email
      end
      click_on 'Groups'
      click_on 'Remove'
      click_on 'System-Admins'
      wait_until do
        not (find("table.users").text.include? @user.email)
      end
    end

  end

end
