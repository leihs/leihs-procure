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

      # return if true


      result = query(q, user.id)

      puts ">> result: " + result.to_s

      expect(result['data']['update_templates']).to be_blank
      expect(result['errors'].first['message']).to match(/UnauthorizedException/)
      expect(Template.all.count).to be == templates_before.count
      templates_before.each do |data|
        puts ">>> data=" + data.to_s
        puts ">>> Template.find(data)=" + Template.find(data).to_s
        expect(Template.find(data)).to be
      end
    end

    # response:
    #   "{\"data\":{\"update_templates\":null},\"errors\":[{\"message\":\"UnauthorizedException - Not authorized for this query path and arguments.\",\"locations\":[{\"line\":2,\"column\":11}],\"path\":[\"update_templates\"],\"extensions\":{\"exception\":\"ExceptionInfo\",\"arguments\":{\"input_data\":[{\"id\":\"f6bd4394-3b64-4d82-bd9d-1c76e01e94d1\",\"article_name\":\"test\",\"category_id\":\"2ab3757b-1517-44c6-9ab2-959375f7b551\",\"price_cents\":100},{\"id\":\"abbdd29f-1c81-4ba6-994f-e3e4b4897f74\",\"article_name\":\"test\",\"category_id\":\"87b5758b-5edd-4cab-9f5d-3467087d3495\",\"price_cents\":100}]}}}]}"
    #
    # >> result: {"data"=>{"update_templates"=>nil}, "errors"=>[{"message"=>"UnauthorizedException - Not authorized for this query path and arguments.", "locations"=>[{"line"=>2, "column"=>11}], "path"=>["update_templates"], "extensions"=>{"exception"=>"ExceptionInfo", "arguments"=>{"input_data"=>[{"id"=>"f6bd4394-3b64-4d82-bd9d-1c76e01e94d1", "article_name"=>"test", "category_id"=>"2ab3757b-1517-44c6-9ab2-959375f7b551", "price_cents"=>100}, {"id"=>"abbdd29f-1c81-4ba6-994f-e3e4b4897f74", "article_name"=>"test", "category_id"=>"87b5758b-5edd-4cab-9f5d-3467087d3495", "price_cents"=>100}]}}}]}
    # >>> data={:article_name=>"tmpl for category A", :category_id=>"2ab3757b-1517-44c6-9ab2-959375f7b551"}
    # >>> Template.find(data)=#<Template:0x000000010e6a78e0>
    #   >>> data={:article_name=>"tmpl for category B", :category_id=>"87b5758b-5edd-4cab-9f5d-3467087d3495"}
    # >>> Template.find(data)=#<Template:0x000000010e6a60f8>
    #   throws if not inspector of some category
    #
    # Finished in 2.04 seconds (files took 0.77458 seconds to load)
    # 1 example, 0 failures


    context 'throws for used templates' do
      before :each do
        @category_A = FactoryBot.create(:category)
        @user = FactoryBot.create(:user)
        FactoryBot.create(:category_inspector,
                          user: @user,
                          category: @category_A)

        @tmpl = FactoryBot.create(:template,
                                  article_name: 'tmpl for category A',
                                  category_id: @category_A.id )

        FactoryBot.create(:request,
                          article_name: @tmpl.article_name,
                          category_id: @category_A.id,
                          template_id: @tmpl.id)
      end

      it 'throws if delete on a used template' do
        q = <<-GRAPHQL
        mutation {
          update_templates(input_data: [
            { id: "#{@tmpl.id}",
              article_name: "test",
              category_id: "#{@category_A.id}",
              price_cents: 100,
              to_delete: true}
          ]) {
            id
            templates {
              article_name
            }
          }
        }
        GRAPHQL

        result = query(q, @user.id)
        expect(result['data']['update_templates']).to be_blank
        expect(result['errors'].first['message']).to match(/violates foreign key constraint/)
        expect(Template.all.count).to be == 1
        expect(Template.find(id: @tmpl.id, article_name: @tmpl.article_name)).to be
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
        { article_name: 'tmpl to archive',
          category_id: category_A.id,
          is_archived: false },
        { article_name: 'tmpl to unarchive',
          category_id: category_A.id,
          is_archived: true },
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
            { id: "#{Template.find(article_name: 'tmpl to archive').id}",
              category_id: "#{category_A.id}",
              is_archived: true },
            { id: "#{Template.find(article_name: 'tmpl to unarchive').id}",
              category_id: "#{category_A.id}",
              is_archived: false },
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
                { 'article_name' => 'tmpl 1 category A' },
                { 'article_name' => 'tmpl to archive' },
                { 'article_name' => 'tmpl to unarchive' }
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

      expect(Template.all.count).to be == 7
      templates_after = [
        { article_name: 'new tmpl category A',
          category_id: category_A.id },
        { article_name: 'tmpl 1 category A',
          category_id: category_A.id },
        { article_name: 'new art name category A',
          category_id: category_A.id },
        { article_name: 'tmpl to archive',
          category_id: category_A.id,
          is_archived: true },
        { article_name: 'tmpl to unarchive',
          category_id: category_A.id,
          is_archived: false },
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
