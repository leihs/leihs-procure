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
                { id: "#{upload_1.id}", typename: "Upload", to_delete: false },
                { id: "#{upload_2.id}", typename: "Upload", to_delete: true },
                { id: "#{attachment_1.id}", typename: "Attachment", to_delete: false },
                { id: "#{attachment_2.id}", typename: "Attachment", to_delete: true }
              ]
            }) {
              id
              attachments {
                value {
                  id
                  filename
                }
              }
            }
          }
        GRAPHQL

        result = query(q, user.id)

        expect(result['data']['request']['id']).to be == request.id
        attachments = result['data']['request']['attachments']['value']
        expect(attachments.count).to be == 2
        attachments
          .map { |a| a['filename'] }
          .each { |fn| expect(fn).not_to be_empty }
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

    it 'move to another category' do
      admin = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: admin.id)

      inspector = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      new_category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
                        user_id: inspector.id,
                        category_id: category.id)

      requester = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: requester.id)

      %w[admin inspector requester].each do |user_name|
        user = binding.local_variable_get(user_name)
        request = FactoryBot.create(:request,
                                    user_id: requester.id,
                                    category_id: category.id,
                                    approved_quantity: 1,
                                    inspection_comment: Faker::Lorem.sentence,
                                    inspector_priority: 'low',
                                    order_quantity: 1)

        q = <<-GRAPHQL
            mutation changeRequestCategory($input: RequestCategoryInput!) {
              change_request_category(input_data: $input) {
                id
                category {
                  value {
                    id
                  }
                }
              }
            }
          GRAPHQL

        variables = { input: { id: request.id, category: new_category.id } }

        result = query(q, user.id, variables)

        expect(result).to be == {
          'data' => {
            'change_request_category' => {
              'id' => request.id,
              'category' => {
                'value' => {
                  'id' => new_category.id
                }
              }
            }
          }
        }

        request.reload
        expect(request.approved_quantity).to be_nil
        expect(request.inspection_comment).to be_nil
        expect(request.inspector_priority).to eq('medium')
        expect(request.order_quantity).to be_nil
      end
    end

    it 'move to another budget period' do
      admin = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: admin.id)

      inspector = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
                        user_id: inspector.id,
                        category_id: category.id)

      requester = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: requester.id)

      new_budget_period = FactoryBot.create(:budget_period)

      %w[admin inspector requester].each do |user_name|
        user = binding.local_variable_get(user_name)
        request = FactoryBot.create(:request,
                                    user_id: requester.id,
                                    category_id: category.id,
                                    inspection_comment: Faker::Lorem.sentence,
                                    inspector_priority: :high,
                                    approved_quantity: 1,
                                    order_quantity: 1)

        q = <<-GRAPHQL
            mutation changeRequestBudgetPeriod($input: RequestBudgetPeriodInput) {
              change_request_budget_period(input_data: $input) {
                id
                budget_period {
                  value {
                    id
                  }
                }
              }
            }
          GRAPHQL

        variables = { input: { id: request.id, budget_period: new_budget_period.id } }

        result = query(q, user.id, variables)

        expect(result).to be == {
          'data' => {
            'change_request_budget_period' => {
              'id' => request.id,
              'budget_period' => {
                'value' => {
                  'id' => new_budget_period.id
                }
              }
            }
          }
        }

        new_request = request.reload
        expect(new_request.inspection_comment).to eq request.inspection_comment
        expect(new_request.inspector_priority).to eq request.inspector_priority
        expect(new_request.approved_quantity).to eq request.approved_quantity
        expect(new_request.order_quantity).to eq request.order_quantity
      end
    end

  end
end
