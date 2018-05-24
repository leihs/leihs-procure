require 'spec_helper'
require_relative 'graphql_helper'

describe 'categories viewers' do
  context 'mutation' do
    before :example do
      @users_before = [
        { firstname: 'user_1' },
        { firstname: 'user_2' },
        { firstname: 'user_3' },
        { firstname: 'inspector' }
      ]

      @users_before.each do |data|
        FactoryBot.create(:user, data)
      end

      @main_categories_before = [
        { name: 'main_cat_1' }
      ]

      @main_categories_before.each do |data|
        FactoryBot.create(:main_category, data)
      end

      @categories_before = [
        { name: 'cat_1', parent: { name: 'main_cat_1' } },
        { name: 'cat_2', parent: { name: 'main_cat_1' } }
      ]

      @categories_before.each do |data|
        p = MainCategory.find(data[:parent])
        FactoryBot.create(:category,
                          name: data[:name],
                          main_category_id: p.id)
      end

      @categories_viewers_before = [
        { user: { firstname: 'user_2' }, category: { name: 'cat_1' } },
        { user: { firstname: 'user_3' }, category: { name: 'cat_2' } }
      ]

      @categories_viewers_before.each do |data|
        FactoryBot.create(
          :category_viewer,
          user_id: User.find(data[:user]).id,
          category_id: Category.find(data[:category]).id
        )
      end

      @q = <<-GRAPHQL
        mutation {
          categories_viewers (
            input_data: [
              { id: "#{Category.find(name: 'cat_1').id}",
                viewers: [
                  "#{User.find(firstname: 'user_1').id}",
                  "#{User.find(firstname: 'user_2').id}",
                  "#{User.find(firstname: 'user_3').id}"
                ] },
              { id: "#{Category.find(name: 'cat_2').id}",
                viewers: [] }
            ]
          ) {
            id
          } 
        }
      GRAPHQL
    end

    it 'returns error if not an inspector of a specific category' do
      categories_inspectors_before = [
        { user: { firstname: 'inspector' }, category: { name: 'cat_1' } }
      ]

      categories_inspectors_before.each do |data|
        FactoryBot.create(
          :category_inspector,
          user_id: User.find(data[:user]).id,
          category_id: Category.find(data[:category]).id
        )
      end

      result = query(@q, User.find(firstname: 'inspector').id)

      expect(result['data']['categories_viewers']).to be_empty
      expect(result['errors'].first['exception']).to be == 'UnauthorizedException'

      CategoryViewer
        .all
        .zip(@categories_viewers_before)
        .each do |cv1, cv2|
          expect(User.find(id: cv1.user_id).firstname)
            .to be == cv2[:user][:firstname]
          expect(Category.find(id: cv1.category_id).name)
            .to be == cv2[:category][:name]
        end
    end

    it 'updates successfully' do
      categories_inspectors_before = [
        { user: { firstname: 'inspector' }, category: { name: 'cat_1' } },
        { user: { firstname: 'inspector' }, category: { name: 'cat_2' } }
      ]

      categories_inspectors_before.each do |data|
        FactoryBot.create(
          :category_inspector,
          user_id: User.find(data[:user]).id,
          category_id: Category.find(data[:category]).id
        )
      end

      result = query(@q, User.find(firstname: 'inspector').id)

      expect(result).to eq({
        'data' => {
          'categories_viewers' => [
            { 'id' => Category.find(name: 'cat_1').id },
            { 'id' => Category.find(name: 'cat_2').id }
          ]
        }
      })

      categories_viewers_after = [
        { user: { firstname: 'user_1' }, category: { name: 'cat_1' } },
        { user: { firstname: 'user_2' }, category: { name: 'cat_1' } },
        { user: { firstname: 'user_3' }, category: { name: 'cat_1' } }
      ]

      expect(CategoryViewer.all.count).to be == categories_viewers_after.count
      categories_viewers_after.each do |data|
        u = User.find(data[:user])
        c = Category.find(data[:category])
        expect(CategoryViewer.find(user_id: u.id, category_id: c.id)).to be
      end
    end
  end
end
