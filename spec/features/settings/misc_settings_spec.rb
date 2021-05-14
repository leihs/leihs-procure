require 'spec_helper'
require 'pry'

feature 'SMTP-Settings' do

  context 'a plain admin exist' do
    before :each do
      @plain_admin = FactoryBot.create :admin
    end

    context 'a plain_admin' do
      before(:each){@user = @plain_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'updates the Miscellaneous-Settings' do
          click_on "Settings"
          click_on "Miscellaneous"
          wait_until { page.has_content? "logo_url" }
          click_on "Edit"
          fill_in "logo_url", with: "https://my-server/leihs-logo.png"
          fill_in "documentation_link", with: "https://my-server/leihs-docs"
          fill_in "contract_lending_party_string", with: "Me"
          fill_in "custom_head_tag", with: "My Header ???"
          fill_in "time_zone", with: "Berlin"
          fill_in "local_currency_string", with: "CHF"
          fill_in "maximum_reservation_time", with: "500"
          fill_in "timeout_minutes", with: "21"
          check "disable_borrow_section"
          fill_in "disable_borrow_section_message", with: "Borrow is disabled."
          check "disable_manage_section"
          fill_in "disable_manage_section_message", with: "Manage is disabled."
          check "deliver_received_order_notifications"
          fill_in "email_signature", with: "Your awesome Lending Desk"
          check "lending_terms_acceptance_required_for_order"
          fill_in "lending_terms_url", with: "https://example.org/fileadmin/leihs-terms-2000-01-01.pdf"
          click_on "Save"
          sleep 0.5
          wait_until{ all(".modal").empty? }
          visit current_url
          wait_until { page.has_content? "logo_url" }
          expect(find_field('logo_url', disabled: true).value).to eq 'https://my-server/leihs-logo.png'
          expect(find_field('documentation_link', disabled: true).value).to eq "https://my-server/leihs-docs"
          expect(find_field('contract_lending_party_string', disabled: true).value).to eq 'Me'
          expect(find_field('custom_head_tag', disabled: true).value).to eq 'My Header ???'
          expect(find_field('time_zone', disabled: true).value).to eq 'Berlin'
          expect(find_field('local_currency_string', disabled: true).value).to eq 'CHF'
          expect(find_field('maximum_reservation_time', disabled: true).value).to eq '500'
          expect(find_field('timeout_minutes', disabled: true).value).to eq '21'
          expect(find_field('disable_borrow_section', disabled: true)).to be_checked
          expect(find_field('disable_borrow_section_message', disabled: true).value).to eq 'Borrow is disabled.'
          expect(find_field('disable_manage_section', disabled: true)).to be_checked
          expect(find_field('disable_manage_section_message', disabled: true).value).to eq 'Manage is disabled.'
          expect(find_field('deliver_received_order_notifications', disabled: true)).to be_checked
          expect(find_field('email_signature', disabled: true).value).to eq 'Your awesome Lending Desk'
          expect(find_field('lending_terms_acceptance_required_for_order', disabled: true)).to be_checked
          expect(find_field('lending_terms_url', disabled: true).value).to eq 'https://example.org/fileadmin/leihs-terms-2000-01-01.pdf'

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

          get = @http_client.get "/admin/settings/misc/"
          expect(get).to be_success
          expect(get.body["disable_borrow_section"]).to be false

          patch = @http_client.patch "/admin/settings/misc/", {disable_borrow_section: true}.to_json
          expect(patch).to be_success
          expect(patch.body["disable_borrow_section"]).to be true

          get = @http_client.get "/admin/settings/misc/"
          expect(get).to be_success
          expect(get.body["disable_borrow_section"]).to be true

        end

      end

    end
  end
end
