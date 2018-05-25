require 'spec_helper'
require_relative 'graphql_helper'

describe 'request' do
  context 'create' do
    context 'mutation' do
      it 'returns error if not sufficient general permissions' do
        viewer = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        FactoryBot.create(:category_viewer,
                          user_id: viewer.id,
                          category_id: category.id)

        attrs = {
          budget_period_id: FactoryBot.create(:budget_period).id,
          category_id: category.id,
          organization_id: FactoryBot.create(:organization).id,
          requested_quantity: 1,
          room_id: FactoryBot.create(:room).id,
          user_id: viewer.id
        }

        q = <<-GRAPHQL
          mutation {
            request(input_data: #{hash_to_graphql attrs}) {
              id {
                value
              }
            }
          }
        GRAPHQL

        result = query(q, viewer.id)

        expect(result['data']['request']).not_to be
        expect(result['errors'].first['exception'])
          .to be == 'UnauthorizedException'

        expect(Request.find(attrs)).not_to be
      end

      it 'creates if required general permission exist' do
        admin = FactoryBot.create(:user)
        FactoryBot.create(:admin, user_id: admin.id)

        inspector = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        FactoryBot.create(:category_inspector,
                          user_id: inspector.id,
                          category_id: category.id)

        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)

        attrs = {
          budget_period_id: FactoryBot.create(:budget_period).id,
          category_id: category.id,
          organization_id: FactoryBot.create(:organization).id,
          requested_quantity: 1,
          room_id: FactoryBot.create(:room).id
        }

        ['admin', 'inspector', 'requester'].each do |user_name|
          user = binding.local_variable_get(user_name)

          attrs2 = attrs.merge(article_name: user_name,
                               user_id: user.id)

          q = <<-GRAPHQL
            mutation {
              new_request(input_data: #{hash_to_graphql attrs2}) {
                id {
                  value
                }
              }
            }
          GRAPHQL

          result = query(q, user.id)

          request = Request.find(attrs2)
          expect(result).to be == {
            'data' => {
              'new_request' => {
                'id' => {
                  'value' => request.id
                }
              }
            }
          }
        end
      end
    end
  end

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

      ['admin', 'inspector', 'requester'].each do |user_name|
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

    context 'delete' do
      context 'mutation' do
        it 'returns error if not sufficient general permissions' do
          requester = FactoryBot.create(:user)
          FactoryBot.create(:requester_organization, user_id: requester.id)
          viewer = FactoryBot.create(:user)
          category = FactoryBot.create(:category)
          FactoryBot.create(:category_viewer,
                            user_id: viewer.id,
                            category_id: category.id)

          request = FactoryBot.create(:request)

          q = <<-GRAPHQL
            mutation {
              delete_request(input_data: {
                id: "#{request.id}"
              }) 
            }
          GRAPHQL

          [requester, viewer].each do |user|
            result = query(q, user.id)

            expect(result['data']['delete_request']).to be_nil
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

        ['admin', 'inspector', 'requester'].each do |user_name|
          user = binding.local_variable_get(user_name)
          request = FactoryBot.create(:request,
                                      user_id: requester.id,
                                      category_id: category.id)

          q = <<-GRAPHQL
            mutation {
              delete_request(input_data: {
                id: "#{request.id}"
              }) 
            }
          GRAPHQL

          result = query(q, user.id)
          expect(result).to be == {
            'data' => {
              'delete_request' => true
            }
          }

          expect(Request.find(id: request.id)).not_to be
        end
      end
    end
  end
end
