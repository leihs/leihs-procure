require 'spec_helper'
require_relative 'graphql_helper'

describe 'settings' do
  context 'mutation' do
    it 'updates successfully' do
      settings = FactoryBot.create(:settings)
      user = User.find(id: FactoryBot.create(:admin).user_id)

      q = <<-GRAPHQL
        mutation {
          settings(input_data: {
            contact_url: "test",
            inspection_comments: [
              "foo",
              "bar",
              "baz"
            ]}
          ) {
            contact_url
            inspection_comments
          }}
      GRAPHQL

      result = query(q, user.id)
      expect(result).to eq({
        'data' => {
          'settings' => {
            'contact_url' => 'test',
            'inspection_comments' => ['foo', 'bar', 'baz']
          }
        }
      })
    end
  end
end
