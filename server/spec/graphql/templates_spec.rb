require 'spec_helper'
require_relative 'graphql_helper'

describe 'templates' do
  context 'mutation' do
    it 'throws if not inspector of some category' do
      category_A = FactoryBot.create(:category)
      category_B = FactoryBot.create(:category)
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector,
                        user: user,
                        category: category_A)

      templates_before = [
        { article_name: 'tmpl for category A',
          category_id: category_A.id },
        { article_name: 'tmpl for category B',
          category_id: category_B.id }
      ]
      templates_before.each do |data|
        FactoryBot.create(:template, data)
      end

      q = <<-GRAPHQL
        mutation {
          update_templates(input_data: [
            { id: "#{Template.find(article_name: 'tmpl for category A').id}",
              article_name: "test",
              category_id: "#{category_A.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: 'tmpl for category B').id}",
              article_name: "test",
              category_id: "#{category_B.id}",
              price_cents: 100 }
          ]) {
            id
            templates {
              article_name
            }
          }
        }
      GRAPHQL

      result = query(q, user.id)
      expect(result['data']['update_templates']).to be_empty
      expect(result['errors'].first['exception'])
        .to be == 'UnauthorizedException'

      expect(Template.all.count).to be == templates_before.count
      templates_before.each do |data|
        expect(Template.find(data)).to be
      end
    end

    it 'updates correctly' do
      category_A = FactoryBot.create(:category, name: 'category A')
      category_B = FactoryBot.create(:category, name: 'category B')
      other_category = FactoryBot.create(:category, name: 'other category')
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector,
                        user: user,
                        category: category_A)
      FactoryBot.create(:category_inspector,
                        user: user,
                        category: category_B)

      templates_before = [
        { article_name: 'tmpl 1 category A',
          category_id: category_A.id },
        { article_name: 'tmpl 2 category A',
          category_id: category_A.id },
        { article_name: 'tmpl to delete category A',
          category_id: category_A.id },
        { article_name: 'tmpl 1 category B',
          category_id: category_B.id },
        { article_name: 'tmpl other category',
          category_id: other_category.id }
      ]
      templates_before.each do |data|
        FactoryBot.create(:template, data)
      end

      q = <<-GRAPHQL
        mutation {
          update_templates(input_data: [
            { id: null,
              article_name: "new tmpl category A",
              category_id: "#{category_A.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: 'tmpl 1 category A').id}",
              article_name: "tmpl 1 category A",
              category_id: "#{category_A.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: 'tmpl to delete category A').id}",
              category_id: "#{category_A.id}",
              to_delete: true },
            { id: "#{Template.find(article_name: 'tmpl 2 category A').id}",
              article_name: "new art name category A",
              category_id: "#{category_A.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: 'tmpl 1 category B').id}",
              article_name: "new art name category B",
              category_id: "#{category_B.id}",
              price_cents: 100 }
          ]) {
            name
            templates {
              article_name
            }
          }
        }
      GRAPHQL

      result = query(q, user.id)

      expect(result).to eq({
        'data' => {
          'update_templates' => [
            { 'name' => 'category A',
              'templates' => [
                { 'article_name' => 'new art name category A' },
                { 'article_name' => 'new tmpl category A' },
                { 'article_name' => 'tmpl 1 category A' }
              ]
            },
            { 'name' => 'category B',
              'templates' => [
                { 'article_name' => 'new art name category B' }
              ]
            }
          ]
        }
      })

      expect(Template.all.count).to be == 5
      templates_after = [
        { article_name: 'new tmpl category A',
          category_id: category_A.id },
        { article_name: 'tmpl 1 category A',
          category_id: category_A.id },
        { article_name: 'new art name category A',
          category_id: category_A.id },
        { article_name: 'new art name category B',
          category_id: category_B.id },
        { article_name: 'tmpl other category',
          category_id: other_category.id }
      ]
      templates_after.each do |data|
        expect(Template.find(data)).to be
      end
    end
  end
end
