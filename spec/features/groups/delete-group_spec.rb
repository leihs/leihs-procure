require 'spec_helper'
require 'pry'

feature 'Deleting groups', type: :feature do

  context 'some admins, a bunch of users and a bunch of groups exist' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @users = 100.times.map { FactoryBot.create :user }
      @groups = 100.times.map { FactoryBot.create :group }
    end

    context 'an admin via the UI' do

      before :each do
        @admin = @admins.sample
        sign_in_as @admin
      end

      scenario "deletes a protected group" do
        @group = @groups.filter{|g| g[:protected] == true }.sample
        expect(database[:groups].where(id: @group.id).first).to be
        visit '/admin/'
        click_on 'Groups'
        fill_in 'Search', with: @group.name
        click_on @group.name
        click_on 'Delete'
        wait_until do
          page.has_content? "Delete Group #{@group.name}"
        end
        click_on 'Delete'
        wait_until{ current_path == '/admin/groups/' }
        expect(database[:groups].where(id: @group.id).first).not_to be
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

        scenario "deletes an unprotected protected group" do
          @group = @groups.filter{|g| g[:protected] == false }.sample
          expect(database[:groups].where(id: @group.id).first).to be
          visit '/admin/'
          click_on 'Groups'
          fill_in 'Search', with: @group.name
          click_on @group.name
          click_on 'Delete'
          wait_until do
            page.has_content? "Delete Group #{@group.name}"
          end
          click_on 'Delete'
          wait_until{ current_path == '/admin/groups/' }
          expect(database[:groups].where(id: @group.id).first).not_to be
        end

        scenario 'can not delete a protected group' do
          @group = @groups.filter{|g| g[:protected] == true }.sample
          expect(database[:groups].where(id: @group.id).first).to be
          visit '/admin/'
          click_on 'Groups'
          fill_in 'Search', with: @group.name
          click_on @group.name
          expect(all("a, button", text: "Delete").count).to be== 0
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

          scenario 'tries to delete a protected group which is forbidden ' do
            @group = @groups.filter{|u| u[:protected]==true }.sample
            resp = http_client.delete "/admin/groups/#{@group[:id]}"
            expect(resp.status).to be== 403
          end

        end
      end
    end
  end
end
