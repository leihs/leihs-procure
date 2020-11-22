require 'spec_helper'
require 'pry'

feature 'Deleting users', type: :feature do

  context 'some admins and a bunch of users exist' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @users = 15.times.map { FactoryBot.create :user }
    end


    context " an admin " do

      before :each do
        @admin = @admins.sample
        sign_in_as @admin
      end

      scenario 'deletes a user via delete ' do

        @to_be_deleted_user = @users.filter {|u|
          u[:is_admin]==false and u[:protected]==true}.sample
        visit '/admin/'
        click_on 'Users'
        fill_in 'Search', with: @to_be_deleted_user.email
        wait_until{ all('tr.user').count == 1 }
        click_on_first_user @to_be_deleted_user
        click_on 'Delete'
        click_on 'Delete'
        wait_until { page.has_content? "No (more) users found." }
        expect(database[:users].where(id: @to_be_deleted_user.id)).to be_empty

      end

      scenario 'deletes a user via transfer and delete' do

        @to_be_deleted_user = @users.filter {|u|
          u[:is_admin]==false and u[:protected]==true}.sample
        @target_user = @users.last
        visit '/admin/'
        click_on 'Users'
        fill_in 'Search', with: @to_be_deleted_user.email
        wait_until{ all('tr.user').count == 1 }
        click_on_first_user @to_be_deleted_user
        click_on 'Delete'
        click_on 'Choose user'
        fill_in 'Search', with: @target_user.email
        wait_until{all( "table.users tbody tr").count == 1 }
        click_on 'Choose user'
        expect(find_field("Target user").value).to be== @target_user.email
        click_on 'Transfer and delete'
        fill_in 'Search', with: @to_be_deleted_user.email
        wait_until { page.has_content? "No (more) users found." }

      end

    end


    context "some inventory-pool's lending-manager " do

      before :each do
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'

      end


      context "signs in, and " do

        before(:each){ sign_in_as @lending_manager }

        scenario 'deletes a user via delete ' do

          @to_be_deleted_user = @users.filter {|u|
            u[:is_admin]==false and u[:protected]==false}.sample
          visit '/admin/'
          click_on 'Users'
          fill_in 'Search', with: @to_be_deleted_user.email
          wait_until{all( "table.users tbody tr").count == 1 }
          click_on_first_user @to_be_deleted_user
          click_on 'Delete'
          click_on 'Delete'
          wait_until { page.has_content? "No (more) users found." }
          expect(database[:users].where(id: @to_be_deleted_user.id)).to be_empty

        end

        scenario 'deletes a user via transfer and delete' do

          @to_be_deleted_user = @users.filter {|u|
            u[:is_admin]==false and u[:protected]==false}.sample
          @target_user = @users.last
          visit '/admin/'
          click_on 'Users'
          fill_in 'Search', with: @to_be_deleted_user.email
          wait_until{all( "table.users tbody tr").count == 1 }
          click_on_first_user @to_be_deleted_user
          click_on 'Delete'
          click_on 'Choose user'
          fill_in 'Search', with: @target_user.email
          wait_until{all( "table.users tbody tr").count == 1 }
          click_on 'Choose user'
          expect(find_field("Target user").value).to be== @target_user.email
          click_on 'Transfer and delete'
          fill_in 'Search', with: @to_be_deleted_user.email
          wait_until { page.has_content? "No (more) users found." }

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

        scenario 'to delete protected is forbidden ' do
          @user = @users.filter{|u| u[:is_admin]==false and u[:protected]==true}.sample
          resp = http_client.delete "/admin/users/#{@user[:id]}"
          expect(resp.status).to be== 403
        end

        scenario 'to transfer and delete protected is forbidden ' do
          @user = @users.filter{|u| u[:protected]==true}.sample
          expect(@user).to be
          @target = @users.filter{|u| u[:id] != @user[:id]}.sample
          expect(@target).to be
          resp = http_client.delete "/admin/users/#{@user[:id]}/transfer/#{@target[:id]}"
          expect(resp.status).to be== 403
        end

      end

    end

  end

end
