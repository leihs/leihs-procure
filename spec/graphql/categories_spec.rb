require 'spec_helper'
require_relative 'graphql_helper'

describe 'categories' do
  context 'query' do
    context 'fields' do
      context 'can_delete' do
        it 'false if referenced by requests' do
          cat = FactoryBot.create(:category)
          user = FactoryBot.create(:user)
          FactoryBot.create(:category_inspector,
                            category_id: cat.id,
                            user_id: user.id)

          FactoryBot.create(:request, category_id: cat.id)
          q = <<-GRAPHQL
            query {
              categories {
                id
                can_delete
              }
            }
          GRAPHQL
          result = query(q, user.id)
          expect(result).to eq({
            'data' => {
              'categories' => [
                { 'id' => cat.id,
                  'can_delete' => false }
              ]
            }
          })
        end

        it 'false if referenced by templates' do
          cat = FactoryBot.create(:category)
          user = FactoryBot.create(:user)
          FactoryBot.create(:category_inspector,
                            category_id: cat.id,
                            user_id: user.id)
          FactoryBot.create(:template, category_id: cat.id)
          q = <<-GRAPHQL
            query {
              categories {
                id
                can_delete
              }
            }
          GRAPHQL
          result = query(q, user.id)
          expect(result).to eq({
            'datas' => {
              'categories' => [
                { 'id' => cat.id,
                  'can_delete' => false }
              ]
            }
          })
        end
      end
    end
  end
end
