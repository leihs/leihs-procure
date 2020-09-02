require 'spec_helper'
require 'pry'

feature 'System/Database/Audits download and clean-up', type: :feature do

  context 'Signed in as a System-Admin; two audtis: one older one newer than one year' do
    before :each do
      @system_admin =  FactoryBot.create :system_admin
      sign_in_as @system_admin
      older_audit = FactoryBot.create :legacy_audit,
        created_at: Date.today - 1.year - 2.days,
        comment: "This one is older than a year."
      older_audit = FactoryBot.create :legacy_audit,
        created_at: Date.today - 1.year + 2.days,
        comment: "This one is newer than a year."
    end

    scenario 'downloading audits via the browser' do
      click_on_first 'System'
      click_on_first 'Database'
      click_on_first 'Audits'
      click_on_first 'Continue'

      audits_before_date = current_url.split('/').last
      audits_file = BROWSER_DOWNLOAD_DIR + "/audits_before_" + audits_before_date + ".json"
      File.delete audits_file rescue 1
      click_on_first 'Download'

      # By default we download audits older than one year, check this:
      expect(File.exists? audits_file).to be true
      file_content = JSON.parse(File.read audits_file)
      expect(file_content['legacy-audits'].count).to be== 1 
      expect(file_content['legacy-audits'].first['comment']).to be== "This one is older than a year."
    end

    scenario 'deleting audits via the browser' do

      click_on_first 'System'
      click_on_first 'Database'
      click_on_first 'Audits'
      click_on_first 'Continue'
      click_on_first 'Delete'
      wait_until { first('.modal', text: 'OK') }

      audits = LegacyAudit.all

      # the older audit has been deleted but not the newer one
      expect(audits.count).to be== 1
      expect(audits.first[:comment]).to be== "This one is newer than a year."

    end


    context 'API with full system-admin access' do

      let :http_client do
        plain_faraday_client
      end

      let :prepare_http_client do
        @api_token = FactoryBot.create :api_token, user_id: @system_admin.id, 
          scope_admin_read: true, scope_admin_write: true,
          scope_system_admin_read: true, scope_system_admin_write: true
        @token_secret = @api_token.token_secret
        http_client.headers["Authorization"] = "Token #{@token_secret}"
        http_client.headers["Content-Type"] = "application/json"
      end

      let :audits_before_url do
        '/admin/system/database/audits/before/' \
          + (Date.today - 1.year).iso8601
      end

      before :each do
        prepare_http_client
      end

      scenario 'downloading audits via API' do
        resp = http_client.get audits_before_url  
        expect(resp.status).to be== 200
        downloaded_legacy_audits =  resp.body['legacy-audits']

        # similar as with the browser, see there above
        expect(downloaded_legacy_audits.count).to be== 1 
        expect(downloaded_legacy_audits.first['comment']).to \
          be== "This one is older than a year."

      end


      scenario 'deleting audits via API' do
        resp = http_client.delete audits_before_url  
        expect(resp.status).to be== 204

        audits = LegacyAudit.all

        # the older audit has been deleted but not the newer one
        expect(audits.count).to be== 1
        expect(audits.first[:comment]).to be== "This one is newer than a year."
      end


      context 'API with system-admin read but not write access' do

        before :each do
          @api_token.update(scope_system_admin_write: false)
        end

        scenario 'downloading audits via API still works' do

          resp = http_client.get audits_before_url  
          expect(resp.status).to be== 200
          downloaded_legacy_audits =  resp.body['legacy-audits']

          # similar as with the browser, see there above
          expect(downloaded_legacy_audits.count).to be== 1 
          expect(downloaded_legacy_audits.first['comment']).to \
            be== "This one is older than a year."

        end

        scenario 'deleting audits via API does not work anymore' do
          resp = http_client.delete audits_before_url  
          expect(resp.status).to be== 403
        end

      end

      context 'API with no system-admin  access' do

        before :each do
          @api_token.update(scope_system_admin_write: false, 
                            scope_system_admin_read: false)
        end

        scenario 'downloading audits via API is forbidden' do
          resp = http_client.get audits_before_url  
          expect(resp.status).to be== 403
        end
      end 
    end
  end
end
