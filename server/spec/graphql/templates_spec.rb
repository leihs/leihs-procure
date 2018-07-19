require 'spec_helper'
require_relative 'graphql_helper'

describe 'templates' do
  context 'mutation' do
    it 'updates correctly' do
      category = FactoryBot.create(:category)
      user = FactoryBot.create(:category_inspector, category: category).user

      templates_before = [
        { article_name: 'tmpl 1',
          category_id: category.id },
        { article_name: 'tmpl 2',
          category_id: category.id },
        { article_name: 'tmpl to delete',
          category_id: category.id },
        { article_name: 'other category',
          category_id: FactoryBot.create(:category).id }
      ]
      templates_before.each do |data|
        FactoryBot.create(:template, data)
      end

      q = <<-GRAPHQL
        mutation {
          templates(input_data: [
            { id: null,
              article_name: "new tmpl",
              category_id: "#{category.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: 'tmpl 1').id}",
              article_name: "tmpl 1",
              category_id: "#{category.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: 'tmpl 2').id}",
              article_name: "new art name",
              category_id: "#{category.id}",
              price_cents: 100 }
          ]) {
            article_name
          }
        }
      GRAPHQL

      result = query(q, user.id)

      expect(result).to eq({
        'data' => {
          'templates' => [
            { 'article_name' => 'new tmpl' },
            { 'article_name' => 'tmpl 1' },
            { 'article_name' => 'new art name' }
          ]
        }
      })
    end
  end
end
