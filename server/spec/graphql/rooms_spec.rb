require 'spec_helper'
require_relative 'graphql_helper'

describe 'rooms' do
  example 'general building returns only general room' do
    user = FactoryBot.create(:user)
    FactoryBot.create(:admin, user_id: user.id)

    g_room = FactoryBot.create(:room, :general_from_general)
    g_building = Building.find(id: g_room.building_id)
    FactoryBot.create(:room, building_id: g_building.id)

    q = <<-GRAPHQL
      query {
        buildings {
          id
          rooms {
            id
          }
        }
      }
    GRAPHQL

    result = query(q, user.id)
    expect(result['data']['buildings'].count).to eq(1)
    b = result['data']['buildings'].first
    expect(b['id']).to eq(g_building.id)
    expect(b['rooms'].count).to eq(1)
    r = b['rooms'].first
    expect(r['id']).to eq(g_room.id)
  end
end
