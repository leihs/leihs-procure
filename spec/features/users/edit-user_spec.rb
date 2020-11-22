require 'spec_helper'
require 'pry'

feature 'Editing users', type: :feature do

  context 'a bunch of users and an admin exist' do

    before :each do
      @admin = FactoryBot.create :admin
      @users = 100.times.map { FactoryBot.create :user }
    end

    context 'an admin' do

      before :each do
        sign_in_as @admin
      end

      scenario 'edits a user to be a protected admin' do

        @user = @users.filter{|u| u[:is_admin]==false and u[:protected]==false}.sample

        visit '/admin/'
        click_on 'Users'
        fill_in 'Search', with: @user.email
        wait_until{ all(".users tbody tr").count == 1 }
        click_on_first_user @user
        expect(find("dl", text: 'Protected')).to have_content 'no'
        expect(find("dl", text: 'Admin')).to have_content 'no'
        click_on 'Edit'
        expect(find(:checkbox, id: 'protected')).not_to be_checked
        expect(find(:checkbox, id: 'is_admin')).not_to be_checked
        check 'is_admin'
        check 'protected'
        click_on 'Save'
        wait_until do
          current_path.match "^\/admin\/users\/.+"
        end
        expect(find("dl", text: 'Protected')).to have_content 'yes'
        expect(find("dl", text: 'Admin')).to have_content 'yes'

      end
    end

    context "some inventory-pool's lending-manager " do

      before :each do
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'
      end

      context "via the UI" do
        before(:each) { sign_in_as @lending_manager }

        scenario "edits an unprotected user" do
          @user = @users.filter{|u| u[:is_admin]==false and u[:protected]==false}.sample
          visit '/admin/'
          click_on 'Users'
          fill_in 'Search' , with: @user.email
          wait_until{ all(".users tbody tr").count == 1 }
          click_on_first_user @user
          click_on 'Edit'
          fill_in :firstname, with: "Bobby"
          fill_in :lastname, with: "Foo"
          click_on 'Save'
          wait_until do
            current_path.match "^\/admin\/users\/.+"
          end
          expect(find("dl", text: "Name")).to have_content "Bobby Foo"
        end

        scenario 'can not edit a protected user' do
          @user = @users.filter{|u| u[:is_admin]==false and u[:protected]==true}.sample
          expect(@user).to be
          visit '/admin/'
          click_on 'Users'
          fill_in 'Search', with: @user.email
          wait_until{ all(".users tbody tr").count == 1 }
          click_on_first_user @user
          within(".breadcrumbs-bar") do
            expect(all("a, button", text: "Edit").count).to be== 0
          end
        end

      end

      context 'via API' do

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

        scenario 'changing is_admin is forbidden ' do
          @user = @users.filter{|u| u[:is_admin]==false and u[:protected]==true}.sample
          resp = http_client.patch "/admin/users/#{@user[:id]}", {is_admin: true }.to_json
          expect(resp.status).to be== 403
        end

        scenario 'changing protected is forbidden ' do
          @user = @users.filter{|u| u[:protected]==false}.sample
          resp = http_client.patch "/admin/users/#{@user[:id]}", {protected: true }.to_json
          expect(resp.status).to be== 403
        end

        scenario 'changing some field of a protected user is forbidden ' do
          user = @users.filter{|g| g[:protected] == true }.sample # pick a random user
          expect(user).to be
          resp = http_client.patch "/admin/users/#{user[:id]}", {name: "New Group-Name", description: "BlaBla"}.to_json
          expect(resp.status).to be== 403
        end

      end
    end
  end
end
