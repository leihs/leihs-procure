require 'spec_helper'
require 'pry'

feature 'System-Admins', type: :feature do

  context 'all cases of system_admin membership in the database' do

    before :each do
      @system_admin =  FactoryBot.create :system_admin, 
        email: 'admin@example.com', password: 'secret'


      @system_admin_group = FactoryBot.create :group
      database[:system_admin_groups].insert group_id: @system_admin_group.id
      @user_in_sytem_amdin_group=  FactoryBot.create :user
      database[:groups_users].insert user_id: @user_in_sytem_amdin_group.id, group_id: @system_admin_group.id

      @direct_system_admin_user =  FactoryBot.create :user
      database[:system_admin_users].insert user_id: @direct_system_admin_user.id

      @simple_user=  FactoryBot.create :user

    end


    scenario 'are reflected correctly on the system-admins page' do

      sign_in_as @system_admin

      click_on_first 'System'
      click_on_first 'System-Admins'

      users_table_text = find("table.users").text

      expect(users_table_text).to include @system_admin.email
      expect(users_table_text).to include @direct_system_admin_user.email
      expect(users_table_text).to include @user_in_sytem_amdin_group.email

      expect(users_table_text).not_to include @simple_user.email

    end

    scenario 'a system-admin sees but a simple_user does not see the "System-Admins" link' do

      sign_in_as @system_admin
      click_on_first 'System'
      expect(page).to have_content "System-Admins"

      find('.fa-user-circle').click
      click_on 'Logout'

      sign_in_as @simple_user
      expect(page).to have_content "Users"
      expect(page).not_to have_content "System-Admins"

    end

  end

end
