require 'spec_helper'
require 'pry'

feature 'Patch a user', type: :feature do

  context 'an existing admin user with api token and a regular user ' do

    let :http_client do
      plain_faraday_client
    end

    before :each do
      @admin = FactoryBot.create :admin
      @api_token = FactoryBot.create :api_token, user_id: @admin.id, 
        scope_admin_read: true, scope_admin_write: true
      @token_secret = @api_token.token_secret

      http_client.headers["Authorization"] = "Token #{@token_secret}"
      http_client.headers["Content-Type"] = "application/json"

      @user = FactoryBot.create :user
    end


    scenario 'patching the user properties works and return the updated properties' do

      resp = http_client.patch("/admin/users/#{@user.id}", {firstname: "Max"}.to_json)

      expect(resp.status).to be== 200
      expect(resp.body["firstname"]).to be== 'Max'

    end

  end

end




