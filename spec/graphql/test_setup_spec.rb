require 'spec_helper'
require_relative 'graphql_helper'

describe 'test setup' do
  it 'test client' do
    response = graphql_client.query <<-GRAPHQL
      query {
        requests {
          id
        }
      }
    GRAPHQL
  end
end
