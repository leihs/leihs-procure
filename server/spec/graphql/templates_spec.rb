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

      puts result

      expect(result['data']['update_templates']).to be_blank
      expect(result['errors'].first['message']).to match(/UnauthorizedException/)
      expect(Template.all.count).to be == templates_before.count
      templates_before.each do |data|          # FIXME: this causes error
        puts ">>> data=" + data.to_json
        puts ">>> Template.find(data)=" + Template.find(data).to_s+"<"
        expect(Template.find(data)).to be
      end
    end

    # # TODO: FIXME
    # response:
    #   "{\"data\":{\"update_templates\":null},\"errors\":[{\"message\":\"UnauthorizedException - Not authorized for this query path and arguments.\",\"locations\":[{\"line\":2,\"column\":11}],\"path\":[\"update_templates\"],\"extensions\":{\"exception\":\"ExceptionInfo\",\"arguments\":{\"input_data\":[{\"id\":\"2d7c573c-b96b-4631-a66b-d49335d36c7c\",\"article_name\":\"test\",\"category_id\":\"49512e30-ef5e-4b93-bdc1-f88555257479\",\"price_cents\":100},{\"id\":\"a8ba6b62-c01e-48eb-8de1-a0b7afa4d548\",\"article_name\":\"test\",\"category_id\":\"f914d1b8-c8e9-4ebc-bf15-83c911a64c77\",\"price_cents\":100}]}}}]}"
    #
    # {"data"=>{"update_templates"=>nil}, "errors"=>[{"message"=>"UnauthorizedException - Not authorized for this query path and arguments.", "locations"=>[{"line"=>2, "column"=>11}], "path"=>["update_templates"], "extensions"=>{"exception"=>"ExceptionInfo", "arguments"=>{"input_data"=>[{"id"=>"2d7c573c-b96b-4631-a66b-d49335d36c7c", "article_name"=>"test", "category_id"=>"49512e30-ef5e-4b93-bdc1-f88555257479", "price_cents"=>100}, {"id"=>"a8ba6b62-c01e-48eb-8de1-a0b7afa4d548", "article_name"=>"test", "category_id"=>"f914d1b8-c8e9-4ebc-bf15-83c911a64c77", "price_cents"=>100}]}}}]}
    # >>> data={"article_name":"tmpl for category A","category_id":"49512e30-ef5e-4b93-bdc1-f88555257479"}
    # >>> Template.find(data)=<


    # Run options: include {:locations=>{"./spec/graphql/templates_spec.rb"=>[6]}}
    #
    # templates
    # mutation
    # >> result: {"data"=>{"update_templates"=>nil}, "errors"=>[{"message"=>"UnauthorizedException - Not authorized for this query path and arguments.",
    # "locations"=>[{"line"=>2, "column"=>11}], "path"=>["update_templates"],
    # "extensions"=>{"exception"=>"ExceptionInfo",
    #
    # "arguments"=>
    # {"input_data"=>[
    # {"id"=>"682c749c-7382-40c4-a81b-e8055bf32bc3", "article_name"=>"test", "category_id"=>"18214e1f-9417-42f8-9f39-2d8e7855d034", "price_cents"=>100},
    # {"id"=>"0d2ec666-7c6c-4ae9-8f20-85ec095200be", "article_name"=>"test", "category_id"=>"36894f94-57b7-4e4e-a4e8-1978c5e32287", "price_cents"=>100}]}}}]}

    # >>> data={:article_name=>"tmpl for category A", :category_id=>"18214e1f-9417-42f8-9f39-2d8e7855d034"}
    # >>> Template.find(data)=#<Template:0x000000010d20f720>
    # >>> data={:article_name=>"tmpl for category B", :category_id=>"36894f94-57b7-4e4e-a4e8-1978c5e32287"}
    # >>> Template.find(data)=#<Template:0x000000010d20df38>
    #   throws if not inspector of some category


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
