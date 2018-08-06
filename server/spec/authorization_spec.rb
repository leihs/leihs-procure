require 'spec_helper'
require_relative 'graphql/graphql_helper'

describe 'authorization' do
  it 'returns 403 if not procurement access' do
    q = <<-GRAPHQL
      query {
        admins { 
          id
        }
      }
    GRAPHQL

    response = GraphqlQuery.new(q).perform.response
    expect(response.status).to eq(403)
  end
end
