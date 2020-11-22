require 'spec_helper'
require 'pry'

feature 'Managing group users ', type: :feature do

  context 'an admin user, bunch of other users and groups with users in groups exist' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @admin = @admins.sample
      @users = 100.times.map { FactoryBot.create :user }
      @groups = 100.times.map { FactoryBot.create :group }

      @groups.each do |group|
        @users.shuffle.take(33).each do |user|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end
    end


    context 'an admin via the UI' do

      before(:each) { sign_in_as @admin }

      scenario 'adds and removes users to a protected group ' do

        @group = @groups.filter{|g| g[:protected] == true}.sample
        @user = @users.sample
        db_group_user = database[:groups_users].where(user_id: @user[:id], group_id: @group[:id])
        db_group_user.delete

        visit '/admin/'
        click_on 'Groups'
        fill_in 'Search', with: @group.name
        click_on @group.name
        click_on 'Users'
        select('members and non-members', from: 'Membership')
        fill_in "Search", with: @user.email
        wait_until{ all("table.users tbody tr").count == 1 }
        expect(find("table.users tbody tr")).to have_content "Add"
        expect(db_group_user.first).not_to be
        within("table.users tbody tr"){ click_on "Add" }
        wait_until{ find("table.users tbody tr").has_content? "Remove" }
        expect(db_group_user.first).to be
        within("table.users tbody tr"){ click_on "Remove" }
        wait_until{ find("table.users tbody tr").has_content? "Add" }
        expect(db_group_user.first).not_to be

      end

    end


    context "some inventory-pool's lending-manager " do

      before :each do
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'
      end

      context 'via the UI' do
        before(:each){ sign_in_as @lending_manager }

        scenario 'adds and removes users to a unprotected group ' do

          @group = @groups.filter{|g| g[:protected] == false}.sample
          @user = @users.sample
          db_group_user = database[:groups_users].where(user_id: @user[:id], group_id: @group[:id])
          db_group_user.delete

          visit '/admin/'
          click_on 'Groups'
          fill_in 'Search', with: @group.name
          click_on @group.name
          click_on 'Users'
          select('members and non-members', from: 'Membership')
          fill_in "Search", with: @user.email
          wait_until{ all("table.users tbody tr").count == 1 }
          expect(find("table.users tbody tr")).to have_content "Add"
          expect(db_group_user.first).not_to be
          within("table.users tbody tr"){ click_on "Add" }
          wait_until{ find("table.users tbody tr").has_content? "Remove" }
          expect(db_group_user.first).to be
          within("table.users tbody tr"){ click_on "Remove" }
          wait_until{ find("table.users tbody tr").has_content? "Add" }
          expect(db_group_user.first).not_to be

        end

        scenario 'can view but not change membership for a protected group ' do

          @group = @groups.filter{|g| g[:protected] == true }.sample
          visit '/admin/'
          click_on 'Groups'
          fill_in 'Search', with: @group.name
          wait_until{ all("tbody tr").count == 1 }
          click_on @group.name
          click_on 'Users'
          select('members and non-members', from: 'Membership')
          select(1000, from: "Per page")
          within("table.users tbody") do
            wait_until{ all("button").count >= 100 }
            all("button").each do |button|
              expect(button).to be_disabled # the key property in this spec
            end
          end
          expect( all("button", text: "Remove").count).to be== 33
          expect( all("button", text: "Add").count).to be>= 100-33 # some extra admins, and lending_manager

        end

      end


      context 'via the API' do

        let :http_client do
          plain_faraday_client
        end

        let :prepare_http_client do
          @api_token = FactoryBot.create :api_token, user_id: @lending_manager.id
          @token_secret = @api_token.token_secret
          http_client.headers["Authorization"] = "Token #{@token_secret}"
          http_client.headers["Content-Type"] = "application/json"
        end

        before :each do
          prepare_http_client
        end

        scenario 'adding a user to a protected group is forbidden ' do
          @group = @groups.filter{|g| g[:protected] == true }.sample
          @user = @users.sample
          db_group_user = database[:groups_users].where(user_id: @user[:id], group_id: @group[:id])
          db_group_user.delete
          resp = http_client.put "/admin/groups/#{@group[:id]}/users/#{@user[:id]}"
          expect(resp.status).to be== 403
        end


      end

    end

  end

end
