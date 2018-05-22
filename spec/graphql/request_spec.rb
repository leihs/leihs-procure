require 'spec_helper'
require_relative 'graphql_helper'

describe 'request' do
  context 'update' do
    context 'mutation' do
      it 'returns error if not sufficient general permissions' do
        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)
        viewer = FactoryBot.create(:user)
        FactoryBot.create(:category_viewer, user_id: viewer.id)

        request = FactoryBot.create(:request)

        q = <<-GRAPHQL
          mutation {
            request(input_data: {
              id: "#{request.id}",
              article_name: "test"
            }) {
              id {
                value
              }
            }
          }
        GRAPHQL

        [requester, viewer].each do |user|
          result = query(q, user.id)

          expect(result['data']['request']).not_to be
          expect(result['errors'].first['exception'])
            .to be == 'UnauthorizedException'

          expect(request).to be == request.reload
        end
      end
    end

    it 'updates if required general permission exists' do
      admin = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: admin.id)

      inspector = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
                        user_id: inspector.id,
                        category_id: category.id)

      requester = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: requester.id)

      request = FactoryBot.create(:request,
                                  user_id: requester.id,
                                  category_id: category.id)

      ['requester'].each do |user_name|
      # ['admin', 'inspector', 'requester'].each do |user_name|
        user = binding.local_variable_get(user_name)

        q = <<-GRAPHQL
        mutation {
          request(input_data: {
            id: "#{request.id}",
            article_name: "#{user_name}"
          }) {
            id {
              value
            }
          }
        }
        GRAPHQL

        result = query(q, user.id)
        expect(result).to be == {
          'data' => {
            'request' => {
              'id' => {
                'value' => request.id
              }
            }
          }
        }
        expect(request.reload.article_name).to be == user_name
      end
    end
  end
end
