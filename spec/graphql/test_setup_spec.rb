require 'spec_helper'

describe 'test setup' do
  let(:client) do
    Graphlient::Client.new('http://localhost:3211/procure/graphql')
  end

  it 'test client' do
    response = client.query <<-'GRAPHQL'
      query {
        requests {
          id
        }
      }
    GRAPHQL
  end
end
