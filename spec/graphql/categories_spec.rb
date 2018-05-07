require 'spec_helper'
require_relative 'graphql_helper'

describe 'categories' do
  context 'query' do
    context 'fields' do
      context 'can_delete' do
        it 'false if referenced by requests' do
          cat = FactoryBot.create(:category)
          FactoryBot.create(:request, category_id: cat.id)
          query = <<-GRAPHQL
            query {
              categories {
                id
                can_delete
              }
            }
          GRAPHQL
          response = graphql_client.query(query)
          expect(response.to_h).to be == {
            'data' => {
              'categories' => [
                { 'id' => cat.id,
                  'can_delete' => false }
              ]
            }
          }
        end

        it 'false if referenced by templates' do
          cat = FactoryBot.create(:category)
          FactoryBot.create(:template, category_id: cat.id)
          query = <<-GRAPHQL
            query {
              categories {
                id
                can_delete
              }
            }
          GRAPHQL
          response = graphql_client.query(query)
          expect(response.to_h).to be == {
            'data' => {
              'categories' => [
                { 'id' => cat.id,
                  'can_delete' => false }
              ]
            }
          }
        end
      end
    end
  end
end
