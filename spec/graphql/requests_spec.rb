require 'spec_helper'
require_relative 'graphql_helper'

describe 'requests' do
  it 'gets data' do
    user = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: user.id)
    request = FactoryBot.create(:request, user_id: user.id)
    q = <<-GRAPHQL
      query {
        requests {
          id
        }
      }
    GRAPHQL
    result = query(q, user.id)
    expect(result).to be == {
      'data' => {
        'requests' => [
          { 'id' => request.id }
        ]
      }
    }
  end
end
