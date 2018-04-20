require 'spec_helper'

feature 'Sign in via shibboleth routes' do

  let :http_client do
    plain_faraday_client
  end

  before :each do
    @user = FactoryBot.create :admin
    database[:settings].insert(shibboleth_enabled: true)
  end

  let :sign_in_response do
    http_client.get('/auth/shib-sign-in') do |req|
      req.headers['mail'] = @user.email
      req.headers['givenname'] = @user.firstname
      req.headers['surname'] = @user.lastname
      req.headers['uniqueid'] = '12345'
    end

  end

  scenario 'sign-in with expected headers sets the session cookie' do
    expect(sign_in_response.headers['set-cookie']).to match /leihs-user-session/
  end


  context 'shibboleth is disabled' do

    before :each do
      database[:settings].update(shibboleth_enabled: false)
    end

    scenario 'sign-in even with expected headers is forbidden' do
      expect(sign_in_response.status).to be== 403
    end

  end

end
