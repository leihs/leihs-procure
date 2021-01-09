require 'spec_helper'
require 'pry'

feature 'System and Security-Settings' do

  context 'a system_admin and a plain admin exist' do
    before :each do
      @system_admin = FactoryBot.create :system_admin
      @plain_admin = FactoryBot.create :admin
    end

    context 'a system_admin' do
      before(:each){@user = @system_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'updates the System and Security-Settings' do
          click_on "Settings"
          click_on "System and Security"
          wait_until { page.has_content? "Base URL" }
          click_on "Edit"
          fill_in "Base URL", with: "https://my-server"
          fill_in "sessions_max_lifetime_secs", with: "3600"
          check "sessions_force_secure"
          check "sessions_force_uniquenes"
          click_on "Save"
          sleep 0.5
          wait_until{ all(".modal").empty? }
          visit current_url
          wait_until { page.has_content? "Base URL" }
          expect(find_field('sessions_max_lifetime_secs', disabled: true).value).to eq '3600'
          expect(find_field('sessions_force_secure', disabled: true)).to be_checked
          expect(find_field('sessions_force_uniquenes', disabled: true)).to be_checked
        end

      end


      context 'via the API' do

        before :each do
          @http_client = plain_faraday_client
          @api_token = FactoryBot.create :system_admin_api_token,
            user_id: @user.id
          @token_secret = @api_token.token_secret
          @http_client.headers["Authorization"] = "Token #{@token_secret}"
          @http_client.headers["Content-Type"] = "application/json"
        end

        scenario 'updating a single property via PATCH works' do

          get = @http_client.get "/admin/settings/syssec/"
          expect(get).to be_success
          expect(get.body["sessions_force_uniqueness"]).to be false

          patch = @http_client.patch "/admin/settings/syssec/", {sessions_force_uniqueness: true}.to_json
          expect(patch).to be_success
          expect(patch.body["sessions_force_uniqueness"]).to be true

          get = @http_client.get "/admin/settings/syssec/"
          expect(get).to be_success
          expect(get.body["sessions_force_uniqueness"]).to be true

        end

      end

    end

    context 'a plain leihs_admin (not system_admin)' do
      before(:each){@user = @plain_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'does not have access to the System and Security-Settings' do
          click_on "Settings"
          expect(all("a", text: "System and Security")).to be_empty
        end

      end

      context 'via the API' do

        before :each do
          @http_client = plain_faraday_client
          @api_token = FactoryBot.create :system_admin_api_token,
            user_id: @user.id
          @token_secret = @api_token.token_secret
          @http_client.headers["Authorization"] = "Token #{@token_secret}"
          @http_client.headers["Content-Type"] = "application/json"
        end

        scenario 'getting the System and Security-settings is forbidden ' do
          get = @http_client.get "/admin/settings/syssec/"
          expect(get).not_to be_success
          expect(get.status).to be== 403
        end

        scenario 'updating the System and Security-settings is forbidden' do
          patch = @http_client.patch "/admin/settings/syssec/", {sessions_force_uniqueness: true}.to_json
          expect(patch).not_to be_success
          expect(patch.status).to be== 403
        end
      end
    end
  end
end
