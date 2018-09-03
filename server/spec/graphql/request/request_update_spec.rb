require 'spec_helper'
require_relative '../graphql_helper'
require_relative 'request_helper'

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
              id
            }
          }
        GRAPHQL

        [requester, viewer].each do |user|
          result = query(q, user.id)

          expect(result['data']['request']).not_to be
          expect(result['errors'].length).to be 1
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

        upload_1 = FactoryBot.create(:upload)
        upload_2 = FactoryBot.create(:upload)
        attachment_1 = FactoryBot.create(:attachment, request_id: request.id)
        attachment_2 = FactoryBot.create(:attachment, request_id: request.id)

        q = <<-GRAPHQL
          mutation {
            request(input_data: {
              id: "#{request.id}",
              article_name: "#{user_name}",
              attachments: [
                { id: "#{upload_1.id}", __typename: "Upload", to_delete: false },
                { id: "#{upload_2.id}", __typename: "Upload", to_delete: true },
                { id: "#{attachment_1.id}", __typename: "Attachment", to_delete: false },
                { id: "#{attachment_2.id}", __typename: "Attachment", to_delete: true }
              ]
            }) {
              id
              attachments {
                value {
                  id
                }
              }
            }
          }
        GRAPHQL

        result = query(q, user.id)

        expect(result['data']['request']['id']).to be == request.id
        expect(result['data']['request']['attachments']['value'].count).to be == 2
        expect(Upload.all.count).to be == 0
        expect(Attachment.count).to be == 2
        expect(Attachment.all.map(&:id)).to include attachment_1.id
        expect(request.reload.article_name).to be == user_name

        Attachment.dataset.delete
        Upload.dataset.delete
      end
    end

    example 'updates organization if requester is changed' do
      inspector = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
                        user_id: inspector.id,
                        category_id: category.id)

      new_requester = FactoryBot.create(:user)
      ro = FactoryBot.create(:requester_organization, user_id: new_requester.id)

      request = FactoryBot.create(:request,
                                  category_id: category.id)

      q = <<-GRAPHQL
        mutation {
          request(input_data: {
            id: "#{request.id}",
            user: "#{new_requester.id}"
          }) {
            id
            organization {
              value {
                id
              }
            }
          }
        }
      GRAPHQL

      result = query(q, inspector.id)

      request_data = result['data']['request']
      expect(request_data['id']).to be == request.id
      expect(request_data['organization']['value']['id']).to be == ro.organization_id
      expect(request.reload.organization_id).to eq(ro.organization_id)
    end
  end
end
