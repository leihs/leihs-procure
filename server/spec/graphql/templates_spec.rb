require "spec_helper"
require_relative "graphql_helper"

describe "templates" do
  context "mutation" do
    it "throws if not inspector of some category" do
      category_a = FactoryBot.create(:category)
      category_b = FactoryBot.create(:category)
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector,
        user: user,
        category: category_a)

      templates_before = [
        {article_name: "tmpl for category A",
         category_id: category_a.id},
        {article_name: "tmpl for category B",
         category_id: category_b.id}
      ]
      templates_before.each do |data|
        FactoryBot.create(:template, data)
      end

      q = <<-GRAPHQL
        mutation {
          update_templates(input_data: [
            { id: "#{Template.find(article_name: "tmpl for category A").id}",
              article_name: "test",
              category_id: "#{category_a.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: "tmpl for category B").id}",
              article_name: "test",
              category_id: "#{category_b.id}",
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
      expect(result["data"]["update_templates"]).to be_blank
      expect(result["errors"].first["message"]).to match(/UnauthorizedException/)
      expect(Template.all.count).to be == templates_before.count
      templates_before.each do |data|
        expect(Template.find(data)).to be
      end
    end

    context "throws for used templates" do
      before :each do
        @category_a = FactoryBot.create(:category)
        @user = FactoryBot.create(:user)
        FactoryBot.create(:category_inspector,
          user: @user,
          category: @category_a)

        @tmpl = FactoryBot.create(:template,
          article_name: "tmpl for category A",
          category_id: @category_a.id)

        FactoryBot.create(:request,
          article_name: @tmpl.article_name,
          category_id: @category_a.id,
          template_id: @tmpl.id)
      end

      it "throws if delete on a used template" do
        q = <<-GRAPHQL
        mutation {
          update_templates(input_data: [
            { id: "#{@tmpl.id}",
              article_name: "test",
              category_id: "#{@category_a.id}",
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
        expect(result["data"]["update_templates"]).to be_blank
        expect(result["errors"].first["message"]).to match(/violates foreign key constraint/)
        expect(Template.all.count).to be == 1
        expect(Template.find(id: @tmpl.id, article_name: @tmpl.article_name)).to be
      end
    end

    it "updates correctly" do
      category_a = FactoryBot.create(:category, name: "category A")
      category_b = FactoryBot.create(:category, name: "category B")
      other_category = FactoryBot.create(:category, name: "other category")
      user = FactoryBot.create(:user)
      FactoryBot.create(:category_inspector,
        user: user,
        category: category_a)
      FactoryBot.create(:category_inspector,
        user: user,
        category: category_b)

      templates_before = [
        {article_name: "tmpl 1 category A",
         category_id: category_a.id},
        {article_name: "tmpl 2 category A",
         category_id: category_a.id},
        {article_name: "tmpl to delete category A",
         category_id: category_a.id},
        {article_name: "tmpl to archive",
         category_id: category_a.id,
         is_archived: false},
        {article_name: "tmpl to unarchive",
         category_id: category_a.id,
         is_archived: true},
        {article_name: "tmpl 1 category B",
         category_id: category_b.id},
        {article_name: "tmpl other category",
         category_id: other_category.id}
      ]
      templates_before.each do |data|
        FactoryBot.create(:template, data)
      end

      q = <<-GRAPHQL
        mutation {
          update_templates(input_data: [
            { id: null,
              article_name: "new tmpl category A",
              category_id: "#{category_a.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: "tmpl 1 category A").id}",
              article_name: "tmpl 1 category A",
              category_id: "#{category_a.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: "tmpl to delete category A").id}",
              category_id: "#{category_a.id}",
              to_delete: true },
            { id: "#{Template.find(article_name: "tmpl to archive").id}",
              category_id: "#{category_a.id}",
              is_archived: true },
            { id: "#{Template.find(article_name: "tmpl to unarchive").id}",
              category_id: "#{category_a.id}",
              is_archived: false },
            { id: "#{Template.find(article_name: "tmpl 2 category A").id}",
              article_name: "new art name category A",
              category_id: "#{category_a.id}",
              price_cents: 100 },
            { id: "#{Template.find(article_name: "tmpl 1 category B").id}",
              article_name: "new art name category B",
              category_id: "#{category_b.id}",
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
        "data" => {
          "update_templates" => [
            {"name" => "category A",
             "templates" => [
               {"article_name" => "new art name category A"},
               {"article_name" => "new tmpl category A"},
               {"article_name" => "tmpl 1 category A"},
               {"article_name" => "tmpl to archive"},
               {"article_name" => "tmpl to unarchive"}
             ]},
            {"name" => "category B",
             "templates" => [
               {"article_name" => "new art name category B"}
             ]}
          ]
        }
      })

      expect(Template.all.count).to be == 7
      templates_after = [
        {article_name: "new tmpl category A",
         category_id: category_a.id},
        {article_name: "tmpl 1 category A",
         category_id: category_a.id},
        {article_name: "new art name category A",
         category_id: category_a.id},
        {article_name: "tmpl to archive",
         category_id: category_a.id,
         is_archived: true},
        {article_name: "tmpl to unarchive",
         category_id: category_a.id,
         is_archived: false},
        {article_name: "new art name category B",
         category_id: category_b.id},
        {article_name: "tmpl other category",
         category_id: other_category.id}
      ]
      templates_after.each do |data|
        expect(Template.find(data)).to be
      end
    end
  end
end
