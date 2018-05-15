require 'faraday'
require 'spec_helper'

describe 'authorization' do
  it 'returns 403 if not procurement access' do
    user = FactoryBot.create(:user)
    response = Faraday.get("#{ENV['LEIHS_HTTP_BASE_URL']}/procure")
    expect(response.status).to be == 403
  end
end
