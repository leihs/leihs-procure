require 'spec_helper'
require_relative 'graphql_helper'

describe 'main categories' do
  let(:admin_user) do
    u = FactoryBot.create(:user)
    FactoryBot.create(:admin, user_id: u.id)
    u
  end

  before :example do
    @users_before = [
      { firstname: 'user_1' },
      { firstname: 'user_2' },
      { firstname: 'user_3' },
      { firstname: 'user_4' },
    ]
    @users_before.each do |data|
      FactoryBot.create(:user, data)
    end

    #############################################################################

    @budget_periods_before = [
      { name: 'budget_period_1' },
      { name: 'budget_period_2' }
    ]
    @budget_periods_before.each do |data|
      FactoryBot.create(:budget_period, data)
    end

    #############################################################################

    @main_categories_before = [
      { name: 'main_cat_1' },
      { name: 'main_cat_2' },
      { name: 'main_cat_to_delete' }
    ]
    @main_categories_before.each do |data|
      FactoryBot.create(:main_category, name: data[:name])
    end

    #############################################################################

    @categories_before = [
      { name: 'cat_1_for_main_cat_1',
        parent: { name: 'main_cat_1' },
        general_ledger_account: 'LEDG_ACC_OLD',
        cost_center: 'CC_OLD' },
        { name: 'cat_to_delete',
          parent: { name: 'main_cat_1' } },
          { name: 'cat_1_for_main_cat_to_delete',
            parent: { name: 'main_cat_to_delete' } }
          ]
    @categories_before.each do |data|
      FactoryBot.create(
        :category,
        name: data[:name],
        main_category_id: MainCategory.find(data[:parent]).id
      )
    end

    #############################################################################

    @category_inspectors_before = [
      { user_id: User.find(firstname: 'user_1').id,
        category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
        { user_id: User.find(firstname: 'user_2').id,
          category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
          { user_id: User.find(firstname: 'user_3').id,
            category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
            { user_id: User.find(firstname: 'user_1').id,
              category_id: Category.find(name: 'cat_1_for_main_cat_to_delete').id }
            ]
    @category_inspectors_before.each do |data|
      FactoryBot.create(:category_inspector, data)
    end

    #############################################################################

    @category_viewers_before = [
      { user_id: User.find(firstname: 'user_1').id,
        category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
        { user_id: User.find(firstname: 'user_2').id,
          category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
          { user_id: User.find(firstname: 'user_3').id,
            category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
            { user_id: User.find(firstname: 'user_1').id,
              category_id: Category.find(name: 'cat_1_for_main_cat_to_delete').id }
            ]
    @category_viewers_before.each do |data|
      FactoryBot.create(:category_viewer, data)
    end

    #############################################################################

    @budget_limits_before = [
      { main_category: { name: 'main_cat_1' },
      budget_period: { name: 'budget_period_1' },
      amount_cents: 50 },
      { main_category: { name: 'main_cat_1' },
      budget_period: { name: 'budget_period_2' },
      amount_cents: 100 },
      { main_category: { name: 'main_cat_to_delete' },
      budget_period: { name: 'budget_period_1' },
      amount_cents: 50 },
      { main_category: { name: 'main_cat_to_delete' },
      budget_period: { name: 'budget_period_2' },
      amount_cents: 100 }
    ]
    @budget_limits_before.each do |data|
      FactoryBot.create(
        :budget_limit,
        main_category_id: MainCategory.find(data[:main_category]).id,
        budget_period_id: BudgetPeriod.find(data[:budget_period]).id,
        amount_cents: data[:amount_cents]
      )
    end

    @images_before = [
      { main_category: { name: 'main_cat_1' } }
    ]
    @images_before.each do |data|
      FactoryBot.create(
        :image,
        filename: 'foo.jpg',
        real_filename: 'lisp-machine.jpg',
        main_category_id: MainCategory.find(data[:main_category]).id
      )
    end

    @upload_1 = FactoryBot.create(:upload)
    @upload_2 = FactoryBot.create(:upload,
      real_filename: 'lisp-machine.jpg',
      content_type: 'image/jpg')
    end

  context 'query' do
    let(:q) do <<-GRAPHQL
      query MainCategories {
        main_categories {
          ...MainCatProps
        }
      }
      fragment MainCatProps on MainCategory {
        id
        name
        can_delete
        image_url
        budget_limits {
          id
          amount_cents
          amount_currency
          budget_period {
            id
            name
            end_date
          }
        }
        categories {
          id
          name
          can_delete
          cost_center
          general_ledger_account
          procurement_account
          inspectors {
            id
            login
            firstname
            lastname
          }
          viewers {
            id
            login
            firstname
            lastname
          }
        }
      }
    GRAPHQL
    end

    example 'successful response' do
      all_cats = MainCategory.all
      expect(all_cats.length).to be > 0

      result = query(q, admin_user.id)

      expect(result['errors']).to be_nil
      expect(result['data']['main_categories'].length).to eq all_cats.length
    end
  end

  context 'mutation' do
    context 'full update' do
      before :each do
        @q = <<-GRAPHQL
          mutation {
            main_categories (
              input_data: [
                { id: null,
                  name: "new_main_cat",
                  budget_limits: [
                    { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_1').id}",
                      amount_cents: 111 },
                    { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_2').id}",
                      amount_cents: 222 }
                  ],
                  categories: [
                    { id: null,
                      name: "new_cat_for_new_main_cat",
                      inspectors: ["#{User.find(firstname: 'user_1').id}",
                                   "#{User.find(firstname: 'user_2').id}"],
                      viewers: ["#{User.find(firstname: 'user_1').id}",
                                "#{User.find(firstname: 'user_2').id}"] }
                  ]
                },
                { id: "#{MainCategory.find(name: 'main_cat_1').id}",
                  name: "main_cat_1",
                  budget_limits: [
                    { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_1').id}",
                      amount_cents: 333 },
                    { budget_period_id: "#{BudgetPeriod.find(name: 'budget_period_2').id}",
                      amount_cents: 444 }
                  ],
                  categories: [
                    { id: "#{Category.find(name: 'cat_1_for_main_cat_1',
                                           main_category_id: MainCategory.find(name: 'main_cat_1').id).id}",
                      name: "cat_1_for_main_cat_1",
                      general_ledger_account: "LEDG_ACC_NEW",
                      cost_center: "CC_NEW",
                      inspectors: ["#{User.find(firstname: 'user_3').id}",
                                   "#{User.find(firstname: 'user_4').id}"],
                      viewers: ["#{User.find(firstname: 'user_3').id}",
                                "#{User.find(firstname: 'user_4').id}"] }
                  ],
                  image: [
                    { id: "#{@upload_1.id}",
                      to_delete: true,
                      __typename: "Upload" },
                    { id: "#{@upload_2.id}",
                      to_delete: false,
                      __typename: "Upload" },
                    { id: "#{Image.find(main_category_id: MainCategory.find(name: 'main_cat_1').id).id}",
                      to_delete: true,
                      __typename: "Image" }
                  ]
                },
                { id: "#{MainCategory.find(name: 'main_cat_2').id}",
                  name: "main_cat_2_new_name" }
              ]
            ) {
              name
            }
          }
        GRAPHQL
      end

      it 'error as inspector' do
        inspector_user = User.find(firstname: 'user_1')
        result = query(@q, inspector_user.id)

        expect(result['data']['main_categories']).to be_empty
        expect(result['errors'].first['exception'])
          .to be == 'UnauthorizedException'

        MainCategory
          .all
          .zip(@main_categories_before)
          .each do |mc1, mc2|
            expect(mc1.name).to be == mc2[:name]
          end

        BudgetPeriod
          .all
          .zip(@budget_periods_before)
          .each do |bp1, bp2|
            expect(bp1.name).to be == bp2[:name]
          end

        Category
          .all
          .zip(@categories_before)
          .each do |c1, c2|
            expect(c1.name).to be == c2[:name]
          end

        CategoryInspector
          .all
          .zip(@category_inspectors_before)
          .each do |ci1, ci2|
            expect(ci1.user_id).to be == ci2[:user_id]
            expect(ci1.category_id).to be == ci2[:category_id]
          end

        CategoryViewer
          .all
          .zip(@category_viewers_before)
          .each do |cv1, cv2|
            expect(cv1.user_id).to be == cv2[:user_id]
            expect(cv1.category_id).to be == cv2[:category_id]
          end

        BudgetLimit
          .all
          .zip(@budget_limits_before)
          .each do |bl1, bl2|
            expect(bl1.main_category.name).to be == bl2[:main_category][:name]
            expect(bl1.budget_period.name).to be == bl2[:budget_period][:name]
            expect(bl1.amount_cents).to be == bl2[:amount_cents]
          end
      end

      it 'success as admin' do
        result = query(@q, admin_user.id)

        expect(result).to eq({
          'data' => {
            'main_categories' => [
              { 'name' => 'main_cat_1' },
              { 'name' => 'main_cat_2_new_name' },
              { 'name' => 'new_main_cat' }
            ]
          }
        })

        #############################################################################

        expect(BudgetPeriod.count).to be == @budget_periods_before.count
        @budget_periods_before.each do |data|
          expect(BudgetPeriod.find(data)).to be
        end

        main_categories_after = [
          { name: 'new_main_cat' },
          { name: 'main_cat_1' },
          { name: 'main_cat_2_new_name' }
        ]
        expect(MainCategory.count).to be == main_categories_after.count
        main_categories_after.each do |data|
          expect(MainCategory.find(data)).to be
        end

        budget_limits_after = [
          { main_category: { name: 'new_main_cat' },
            budget_period: { name: 'budget_period_1' },
            amount_cents: 111 },
          { main_category: { name: 'new_main_cat' },
            budget_period: { name: 'budget_period_2' },
            amount_cents: 222 },
          { main_category: { name: 'main_cat_1' },
            budget_period: { name: 'budget_period_1' },
            amount_cents: 333 },
          { main_category: { name: 'main_cat_1' },
            budget_period: { name: 'budget_period_2' },
            amount_cents: 444 }
        ]
        expect(BudgetLimit.count).to be == budget_limits_after.count
        budget_limits_after.each do |data|
          bl = BudgetLimit.find(
            main_category_id: MainCategory.find(data[:main_category]).id,
            budget_period_id: BudgetPeriod.find(data[:budget_period]).id,
            amount_cents: data[:amount_cents]
          )
          expect(bl).to be
        end

        categories_after = [
          { name: 'cat_1_for_main_cat_1',
            parent: { name: 'main_cat_1' },
            general_ledger_account: 'LEDG_ACC_NEW',
            cost_center: 'CC_NEW' },
          { name: 'new_cat_for_new_main_cat',
            parent: { name: 'new_main_cat' } }
        ]
        expect(Category.count).to be == categories_after.count
        categories_after.each do |data|
          parent = MainCategory.find(data[:parent])
          c = Category.find(
            data
            .reject { |k, _| k == :parent }
            .merge(main_category_id: parent.id)
          )
          expect(c).to be
        end

        category_inspectors_after = [
          { user_id: User.find(firstname: 'user_1').id,
            category_id: Category.find(name: 'new_cat_for_new_main_cat').id },
          { user_id: User.find(firstname: 'user_2').id,
            category_id: Category.find(name: 'new_cat_for_new_main_cat').id },
          { user_id: User.find(firstname: 'user_3').id,
            category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
          { user_id: User.find(firstname: 'user_4').id,
            category_id: Category.find(name: 'cat_1_for_main_cat_1').id }
        ]
        expect(CategoryInspector.count).to be == category_inspectors_after.count
        category_inspectors_after.each do |data|
          expect(CategoryInspector.find(data)).to be
        end

        category_viewers_after = [
          { user_id: User.find(firstname: 'user_1').id,
            category_id: Category.find(name: 'new_cat_for_new_main_cat').id },
          { user_id: User.find(firstname: 'user_2').id,
            category_id: Category.find(name: 'new_cat_for_new_main_cat').id },
          { user_id: User.find(firstname: 'user_3').id,
            category_id: Category.find(name: 'cat_1_for_main_cat_1').id },
          { user_id: User.find(firstname: 'user_4').id,
            category_id: Category.find(name: 'cat_1_for_main_cat_1').id }
        ]
        expect(CategoryViewer.count).to be == category_viewers_after.count
        category_viewers_after.each do |data|
          expect(CategoryViewer.find(data)).to be
        end

        expect(Upload.count).to be == 0
        expect(Image.count).to be == 1
        expect(
          Image.find(
            main_category_id: MainCategory.find(name: 'main_cat_1').id
          ).filename
        ).to be == 'lisp-machine.jpg'
      end

    end
  end
end
