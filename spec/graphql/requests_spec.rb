require 'spec_helper'
require_relative 'graphql_helper'

describe 'requests' do
  it 'gets data' do
    request = FactoryBot.create(:request)
    response = graphql_client.query <<-GRAPHQL
      query {
        requests {
          id
        }
      }
    GRAPHQL
    expect(response.to_h).to be == {
      'data' => {
        'requests' => [
          { 'id' => request.id }
        ]
      }
    }
  end
end
