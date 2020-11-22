require 'spec_helper'
require 'pry'

feature 'Creating users', type: :feature do

  context 'a bunch of users and an admin exist' do

    before :each do
      @admin = FactoryBot.create :admin
      @users = 15.times.map do
        FactoryBot.create :user
      end
    end

    context 'an admin' do

      before :each do
        sign_in_as @admin
      end

      scenario 'creates a new user' do

        visit '/admin/'
        click_on 'Users'
        click_on 'Create user'
        check 'account_enabled'
        check 'password_sign_in_enabled'
        check 'is_admin'
        check 'protected'
        fill_in 'email', with: 'test@example.com'
        create_path = current_path
        click_on 'Create'
        wait_until do
          current_path.match "^\/admin\/users\/.+"
        end

      end

      scenario 'create a new user wo email' do
        visit '/admin/'
        click_on 'Users'
        click_on 'Create user'
        check 'account_enabled'
        check 'password_sign_in_enabled'
        check 'is_admin'
        check 'protected'
        fill_in 'email', with: " "
        fill_in 'login', with: 'test'
        create_path = current_path
        click_on 'Create'
        wait_until do
          current_path.match "^\/admin\/users\/.+"
        end
      end

    end

    context "some inventory-pool's lending-manager " do

      before :each do
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'
        sign_in_as @lending_manager
      end

      scenario 'creates a new user' do

        visit '/admin/'
        click_on 'Users'
        click_on 'Create user'
        check 'account_enabled'
        check 'password_sign_in_enabled'
        expect(find(:checkbox, id: 'is_admin', disabled: true)).not_to be_checked # is_admin is disabled and unchecked
        expect(find(:checkbox, id: 'protected', disabled: true)).not_to be_checked # is_admin is disabled and unchecked
        fill_in 'email', with: 'test@example.com'
        click_on 'Create'
        wait_until do
          current_path.match "^\/admin\/users\/.+"
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

        scenario 'tries to set is_admin forbidden ' do
          post_resp = http_client.post "/admin/users/", {login: "new-user", is_admin: true }.to_json
          expect(post_resp.status).to be== 403
          new_user = User.where(login: 'new-user').first
          expect(new_user).not_to be
        end

        scenario 'tries to set protected forbidden ' do
          post_resp = http_client.post "/admin/users/", {login: "new-user", protected: true }.to_json
          expect(post_resp.status).to be== 403
          new_user = User.where(login: 'new-user').first
          expect(new_user).not_to be
        end


      end

    end

  end

end

