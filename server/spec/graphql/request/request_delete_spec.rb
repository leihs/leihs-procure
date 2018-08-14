require 'spec_helper'
require_relative '../graphql_helper'
require_relative 'request_helper'

describe 'request' do
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
          mutation deleteRequest($input: DeleteRequestInput) {
            delete_request(input_data: $input)
          }
        GRAPHQL

        variables = { input: { id: request.id } }

        [requester, viewer].each do |user|
          result = query(q, user.id, variables)

          expect(result['data']['delete_request']).to be_nil
          expect(result['errors'].first['exception'])
            .to be == 'UnauthorizedException'

          expect(request).to be == request.reload
        end
      end
    end

    it 'deletes if required general permission exists' do
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
          mutation deleteRequest($input: DeleteRequestInput) {
            delete_request(input_data: $input)
          }
        GRAPHQL

        variables = { input: { id: request.id } }

        result = query(q, user.id, variables)
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
