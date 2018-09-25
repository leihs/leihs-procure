require 'spec_helper'
require_relative '../graphql_helper'
require_relative 'request_helper'

describe 'request' do
  let(:budget_period) { FactoryBot.create(:budget_period) }
  let(:category) { FactoryBot.create(:category) }
  let(:template) do
    FactoryBot.create(
      :template,
      article_name: 'some template',
      price_cents: 2300,
      category_id: FactoryBot.create(:category).id
    )
  end
  let :requester do
    requester = FactoryBot.create(:user)
    FactoryBot.create(:requester_organization, user_id: requester.id)
    requester
  end

  context 'new' do
    let :q do
      <<-GRAPHQL
        query newRequestQuery($budgetPeriod: ID!, $category: ID, $template: ID) {
          new_request(
            budget_period: $budgetPeriod
            category: $category
            template: $template
          ) {
            template {
              value {
                id
                article_name
              }
            }

            category {
              value {
                id
                name
              }
            }
            budget_period {
              value {
                id
              }
            }
            article_name {
              ...RequestFieldString
            }
            price_cents {
              ...RequestFieldInt
            }
            motivation {
              ...RequestFieldString
            }
            priority {
              ...RequestFieldPriority
            }
            inspector_priority {
              ...RequestFieldInspectorPriority
            }
            requested_quantity {
              ...RequestFieldInt
            }
            room {
              ...RequestFieldRoom
            }
            approved_quantity {
              ...RequestFieldInt
            }
            state
            supplier_name {
              ...RequestFieldString
            }
            user {
              ...RequestFieldUser
            }
          }
        }

        fragment RequestFieldString on RequestFieldString { value, read, write }
        fragment RequestFieldInt on RequestFieldInt { value, read, write }
        fragment RequestFieldRoom on RequestFieldRoom {
          read
          write
          required
          value { ...RoomProps }
          default { ...RoomProps}
        }
        fragment RequestFieldUser on RequestFieldUser {
          read
          write
          value { id }
          default { id }
        }
        fragment RequestFieldPriority on RequestFieldPriority { value, read, write }
        fragment RequestFieldInspectorPriority on RequestFieldInspectorPriority { value, read, write }
        fragment RoomProps on Room { id general building { id }}
      GRAPHQL
    end

    context 'get data for new request' do
      before do
        @general_room_from_general = FactoryBot.create(:room, :general_from_general)
        # check that user is not inspector for category
        expect(CategoryInspector.find(category_id: category.id, user_id: requester.id)).not_to be
      end

      example 'from category' do
        variables = {
          budgetPeriod: budget_period.id,
          category: category.id
        }
        result = query(q, requester.id, variables).deep_symbolize_keys
        expect(result.deep_symbolize_keys[:data][:new_request])
          .to eq(template: { value: nil },
                 category: { value: { id: category.id, name: category.name } },
                 budget_period: { value: { id: budget_period.id } },
                 article_name: { value: nil, read: true, write: true },
                 price_cents: { value: 0, read: true, write: true },
                 motivation: { value: nil, read: true, write: true },
                 priority: { value: 'NORMAL', read: true, write: true },
                 inspector_priority: { value: nil, read: false, write: false },
                 requested_quantity: { value: nil, read: true, write: true },
                 approved_quantity: { value: nil, read: false, write: false },
                 room: {
                   read: true,
                   write: true,
                   required: true,
                   value: {
                     id: @general_room_from_general.id,
                     building: {
                       id: @general_room_from_general.building_id
                     },
                     general: true
                   },
                   default: {
                     id: @general_room_from_general.id,
                     building: {
                       id: @general_room_from_general.building_id
                     },
                     general: true
                   },
                 },
                 state: 'NEW',
                 supplier_name: { value: nil, read: true, write: true },
                 user: { value: { id: requester.id }, read: true, write: false, default: { id: requester.id } }
                )
      end

      example 'from template' do
        template_category = Category.find(id: template.category_id)
        variables = {
          budgetPeriod: budget_period.id,
          template: template.id
        }
        result = query(q, requester.id, variables).deep_symbolize_keys
        expect(result.deep_symbolize_keys[:data][:new_request])
          .to eq(template: { value: { id: template.id, article_name: template.article_name } },
                 category: { value: { id: template_category.id, name: template_category.name } },
                 budget_period: { value: { id: budget_period.id } },
                 # those come from the template and cant be overridden
                 article_name: { value: template.article_name, read: true, write: false },
                 price_cents: { value: template.price_cents, read: true, write: false },
                 # other form fields:
                 motivation: { value: nil, read: true, write: true },
                 priority: { value: 'NORMAL', read: true, write: true },
                 inspector_priority: { value: nil, read: false, write: false },
                 requested_quantity: { value: nil, read: true, write: true },
                 approved_quantity: { value: nil, read: false, write: false },
                 room: {
                   read: true,
                   write: true,
                   required: true,
                   value: {
                     id: @general_room_from_general.id,
                     building: {
                       id: @general_room_from_general.building_id
                     },
                     general: true
                   },
                   default: {
                     id: @general_room_from_general.id,
                     building: {
                       id: @general_room_from_general.building_id
                     },
                     general: true
                   },
                 },
                 state: 'NEW',
                 supplier_name: { value: nil, read: true, write: false },
                 user: { value: { id: requester.id }, read: true, write: false, default: { id: requester.id } }
                )
      end
    end
  end

  context 'create' do
    let :q do
      <<-GRAPHQL
      mutation createRequest($input: CreateRequestInput) {
        create_request(input_data: $input) {
          id
          article_name {
            value
          }
          attachments {
            value {
              id
            }
          }
          motivation {
            value
          }
          supplier_name {
            value
          }
        }
      }
      GRAPHQL
    end
    let(:minimal_input) do
      {
        article_name: 'new request',
        budget_period: FactoryBot.create(:budget_period).id,
        category: FactoryBot.create(:category).id,
        requested_quantity: 1,
        room: FactoryBot.create(:room).id,
        motivation: Faker::Lorem.sentence,
      }
    end

    it 'returns error if not sufficient general permissions' do
      viewer = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_viewer,
                        user_id: viewer.id,
                        category_id: category.id)

      attrs = minimal_input.merge({
        article_name: 'new request',
        user: viewer.id
      })

      variables = { input: attrs }
      result = query(q, viewer.id, variables)

      expect(result['data']['request']).not_to be
      expect(result['errors'].first['exception'])
        .to be == 'UnauthorizedException'

      expect(Request.find(transform_uuid_attrs(attrs))).not_to be
    end

    context 'creates as requester' do
      let(:uploads) { Array.new(2) { FactoryBot.create(:upload) } }


      example 'for category/no template' do
        variables = {
          input: minimal_input.merge({
            article_name: 'new request',
            attachments: [{ id: uploads[0].id, to_delete: false, typename: 'Upload' },
                          { id: uploads[1].id, to_delete: true, typename: 'Upload' }],
            supplier_name: 'OBike'
          })
        }

        result = query(q, requester.id, variables).deep_symbolize_keys

        request = Request.order(:created_at).reverse.first
        expect(result[:errors]).to be_nil
        data = result[:data][:create_request]
        expect(data[:id]).to be == request.id
        expect(data[:attachments][:value].count).to be == 1
        expect(data[:supplier_name][:value]).to eq('OBike')
        expect(Upload.count).to be == 0
        expect(Attachment.count).to be == 1
      end

      example 'from template' do
        variables = {
          input: minimal_input.without(:article_name).merge({
            template: template.id,
            attachments: [{ id: uploads[0].id, to_delete: false, typename: 'Upload' },
                          { id: uploads[1].id, to_delete: true, typename: 'Upload' }]
          })
        }

        result = query(q, requester.id, variables).deep_symbolize_keys

        request = Request.order(:created_at).reverse.first
        expect(result[:errors]).to be_nil
        data = result[:data][:create_request]
        expect(data[:id]).to be == request.id
        expect(data[:attachments][:value].count).to be == 1
        expect(data[:article_name][:value]).to eq template[:article_name]
        expect(data[:motivation][:value]).to eq variables[:input][:motivation]
        expect(Upload.count).to be == 0
        expect(Attachment.count).to be == 1
      end

      example 'not allowed if past phase' do
        variables = {
          input: minimal_input.merge({
            budget_period: FactoryBot.create(:budget_period, :past).id,
          })
        }
        expect_error_not_authorized(query(q, requester.id, variables))
      end
    end

    context 'create for another user' do
      before :example do
        @requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: @requester.id)
        @category = FactoryBot.create(:category)
      end

      it 'as admin' do
        @auth_user = User.find(id: FactoryBot.create(:admin).user_id)
        FactoryBot.create(:requester_organization, user_id: @auth_user.id)
      end

      it 'as inspector' do
        @auth_user = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: @auth_user.id)
        FactoryBot.create(:category_inspector,
                          user_id: @auth_user.id,
                          category_id: @category.id)
      end

      after :example do
        attrs = {
          article_name: 'new request',
          budget_period: FactoryBot.create(:budget_period).id,
          category: @category.id,
          requested_quantity: 1,
          room: FactoryBot.create(:room).id,
          motivation: Faker::Lorem.sentence,
          user: @requester.id
        }

        q = <<-GRAPHQL
            mutation createRequst($inputData: CreateRequestInput){
              create_request(input_data: $inputData) {
                id
              }
            }
          GRAPHQL

        result = query(q, @auth_user.id, {inputData: attrs})

        request = Request.order(:created_at).reverse.first
        expect(result['data']['create_request']['id']).to be == request.id
        expect(request.user_id).to eq(@requester.id)
      end
    end

    context 'create as inspector only' do
      let(:requesting_user) do
        user = FactoryBot.create(:user)
        FactoryBot.create(:category_inspector,
                          user_id: user.id,
                          category_id: FactoryBot.create(:category).id)
        user
      end

      example 'not allowed' do
        variables = { input: minimal_input }
        expect_error_not_authorized(query(q, requesting_user.id, variables))
      end
    end

    context 'create as admin only' do
      let(:requesting_user) do
        User.find(id: FactoryBot.create(:admin).user_id)
      end

      example 'not allowed' do
        variables = { input: minimal_input }
        expect_error_not_authorized(query(q, requesting_user.id, variables))
      end
    end


    context 'create as inspector+requester' do
      let(:inspected_category) { FactoryBot.create(:category) }
      let(:other_category) { FactoryBot.create(:category) }
      let(:requesting_user) do
        user = FactoryBot.create(:user)
        FactoryBot.create(:category_inspector,
                          user_id: user.id,
                          category_id: inspected_category.id)
        FactoryBot.create(:requester_organization, user_id: user.id)
        user
      end
      let(:input) do
        minimal_input.merge(category: inspected_category.id)
      end

      example 'in current phase - is allowed' do
        variables = { input: input }
        expect_created_ok(query(q, requesting_user.id, variables))
      end

      example 'in past phase - not allowed' do
        variables = {
          input: input.merge({
            budget_period: FactoryBot.create(:budget_period, :past).id,
          })
        }
        expect_error_not_authorized(query(q, requesting_user.id, variables))
      end

      context 'in inspection phase' do
        let(:r_input) do
          input.merge({
            budget_period: FactoryBot.create(:budget_period, :inspection_phase).id,
          })
        end

        example 'allowed for inspected category' do
          variables = { input: r_input.merge(category: inspected_category.id) }
          result = query(q, requester.id, variables)
          expect_created_ok(query(q, requesting_user.id, variables))
        end

        example 'not allowed for non-inspected category' do
          variables = { input: r_input.merge(category: other_category.id) }
          expect_error_not_authorized(query(q, requesting_user.id, variables))
        end

      end
    end

    context 'create as inspector+requester' do
      let(:inspected_category) { FactoryBot.create(:category) }
      let(:other_category) { FactoryBot.create(:category) }
      let(:requesting_user) do
        user = FactoryBot.create(:user)
        FactoryBot.create(:category_inspector,
                          user_id: user.id,
                          category_id: inspected_category.id)
        FactoryBot.create(:requester_organization, user_id: user.id)
        user
      end
      let(:input) do
        minimal_input.merge(category: inspected_category.id)
      end

      example 'in current phase - is allowed' do
        variables = { input: input }
        expect_created_ok(query(q, requesting_user.id, variables))
      end

      example 'in past phase - not allowed' do
        variables = {
          input: input.merge({
            budget_period: FactoryBot.create(:budget_period, :past).id,
          })
        }
        expect_error_not_authorized(query(q, requesting_user.id, variables))
      end

      context 'in inspection phase' do
        let(:r_input) do
          input.merge({
            budget_period: FactoryBot.create(:budget_period, :inspection_phase).id,
          })
        end

        example 'allowed for inspected category' do
          variables = { input: r_input.merge(category: inspected_category.id) }
          result = query(q, requester.id, variables)
          expect_created_ok(query(q, requesting_user.id, variables))
        end

        example 'not allowed for non-inspected category' do
          variables = { input: r_input.merge(category: other_category.id) }
          expect_error_not_authorized(query(q, requesting_user.id, variables))
        end

      end
    end

    context 'create as admin+requester' do
      let(:inspected_category) { FactoryBot.create(:category) }
      let(:other_category) { FactoryBot.create(:category) }
      let(:requesting_user) do
        user = User.find(id: FactoryBot.create(:admin).user_id)
        FactoryBot.create(:requester_organization, user_id: user.id)
        user
      end
      let(:input) do
        minimal_input.merge(category: inspected_category.id)
      end

      example 'in current phase - is allowed' do
        variables = { input: input }
        expect_created_ok(query(q, requesting_user.id, variables))
      end

      example 'in past phase - not allowed' do
        variables = {
          input: input.merge({
            budget_period: FactoryBot.create(:budget_period, :past).id,
          })
        }
        expect_error_not_authorized(query(q, requesting_user.id, variables))
      end

      context 'in inspection phase' do
        let(:r_input) do
          input.merge({
            budget_period: FactoryBot.create(:budget_period, :inspection_phase).id,
          })
        end

        example 'allowed for inspected category' do
          variables = { input: r_input.merge(category: inspected_category.id) }
          result = query(q, requester.id, variables)
          expect_created_ok(query(q, requesting_user.id, variables))
        end

        example 'allowed for non-inspected category' do
          variables = { input: r_input.merge(category: other_category.id) }
          expect_created_ok(query(q, requesting_user.id, variables))
        end

      end
    end

  end
end


def expect_error_not_authorized(result)
  result = result.deep_symbolize_keys
  result_error = result.fetch(:errors, {}).fetch(0, {})
  expect(result_error[:message]).to include('Not authorized')
  expect(result[:data][:create_request]).to be_nil
end

def expect_created_ok(result)
  result = result.deep_symbolize_keys
  expect(result[:errors]).to be_nil
  result_request = result.fetch(:data, {}).fetch(:create_request, {})
  expect(result_request[:id]).to be
  expect(Request.find(id: result_request[:id])).to be_a Request
end
