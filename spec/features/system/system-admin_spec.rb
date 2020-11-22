require 'spec_helper'
require 'pry'

shared_context :setup_api do
  before :each do
    @http_client = plain_faraday_client
    @api_token = FactoryBot.create :api_token, user_id: @current_user.id,
      scope_system_admin_read: true, scope_system_admin_write: true
    @token_secret = @api_token.token_secret
    @http_client.headers["Authorization"] = "Token #{@token_secret}"
    @http_client.headers["Content-Type"] = "application/json"
  end
end

feature 'System-Admins', type: :feature do

  context "an admin, a system_admin, and a bunch of users exist" do
    before :each do

      @admin =  FactoryBot.create :admin,
        email: 'admin@example.com', password: 'secret'

      @system_admin =  FactoryBot.create :system_admin,
        email: 'system-admin@example.com', password: 'secret',
        is_admin: false

      @users = 100.times.map { FactoryBot.create :user }.to_a

    end

    context 'as the system_admin ' do

      before(:each){ @current_user = @system_admin}

      context 'via the UI' do
        before(:each){ sign_in_as @current_user }

        scenario 'I can click to the system-admins resource via the breadcrums an see excaclty myself listed' do
          click_on 'System'
          click_on 'System-Admins'
          within(".users tbody") do
            expect(find("tr")).to have_content @current_user.email
          end
        end

        scenario 'I can add and remove other users' do
          further_system_admin = @users.sample
          click_on 'System'
          click_on 'System-Admins'
          select 'any', from: 'Is system admin'
          fill_in 'Search', with: further_system_admin.email
          wait_until do
            (all(".users tbody tr").count == 1) and
              find(".users tbody tr").has_content?(further_system_admin.email)
          end
          within(".users tbody tr"){ click_on 'Add'}
          wait_until { all('.modal').empty? }
          wait_until do
            within(".users tbody") do
              find("tr").has_content?("Remove")
            end
          end
          expect(database[:system_admin_users] \
            .where(user_id: further_system_admin.id) \
            .first).to be
          within(".users tbody tr"){ click_on 'Remove'}
          wait_until { all('.modal').empty? }
          wait_until do
            within(".users tbody") do
              find("tr").has_content?("Add")
            end
          end
          expect(database[:system_admin_users] \
            .where(user_id: further_system_admin.id) \
            .first).not_to be
        end

      end

      context 'via the API ' do

        include_context :setup_api
        scenario 'I can add and remove a user to/from the system_admins' do
          further_system_admin = @users.sample
          add_resp = @http_client.put "/admin/system/system-admins/#{further_system_admin[:id]}"
          expect(add_resp.status).to be< 300
          expect(database[:system_admin_users] \
            .where(user_id: further_system_admin.id) \
            .first).to be

          remove_resp = @http_client.delete "/admin/system/system-admins/#{further_system_admin[:id]}"
          expect(remove_resp.status).to be< 300
          expect(database[:system_admin_users] \
            .where(user_id: further_system_admin.id) \
            .first).not_to be
        end

      end

    end

    context 'as the leihs admin and which is not a system_admin ' do
      before(:each){@current_user = @admin}
      context 'via the API' do
        include_context :setup_api

        scenario 'adding a system-admin is forbidden' do
          further_system_admin = @users.sample
          add_resp = @http_client.put "/admin/system/system-admins/#{further_system_admin[:id]}"
          expect(add_resp.status).to be== 403
          expect(database[:system_admin_users] \
            .where(user_id: further_system_admin.id) \
            .first).not_to be

        end

        scenario "removing a system-admin is forbidden" do
          remove_resp = @http_client.delete "/admin/system/system-admins/#{@system_admin[:id]}"
          expect(remove_resp.status).to be== 403
          expect(database[:system_admin_users] \
            .where(user_id: @system_admin[:id]) \
            .first).to be
        end

      end

    end

  end

end
