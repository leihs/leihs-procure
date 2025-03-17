require "spec_helper"
require_relative "../graphql_helper"
require_relative "request_helper"

describe "request" do
  context "update" do
    context "mutation" do
      it "returns error if not sufficient general permissions" do
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

          expect(result["data"]["request"]).not_to be
          expect(result["errors"].length).to be 1
          expect(result["errors"].first["message"]).to match(/UnauthorizedException/)

          expect(request).to be == request.reload
        end
      end
    end

    it "updates if required general permission exists" do
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

      ["admin", "inspector", "requester"].each do |user_name|
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

        expect(result["data"]["request"]["id"]).to be == request.id
        attachments = result["data"]["request"]["attachments"]["value"]
        expect(attachments.count).to be == 2
        attachments
          .map { |a| a["filename"] }
          .each { |fn| expect(fn).not_to be_blank }
        expect(Upload.all.count).to be == 0
        expect(Attachment.count).to be == 2
        expect(Attachment.all.map(&:id)).to include attachment_1.id
        expect(request.reload.article_name).to be == user_name

        pdf_response = get_request("procure/attachments/#{attachment_1.id}")
        expect(pdf_response.status).to be == 200
        expect(pdf_response.headers["Content-Type"]).to be == "application/pdf"
        expect(pdf_response.headers["Content-Disposition"]).to be == "inline; filename=\"#{attachment_1.filename}\""
        expect(pdf_response.headers["content-length"]).to be == attachment_1.size.to_s

        Attachment.dataset.delete
        Upload.dataset.delete
      end
    end

    example "updates organization if requester is changed" do
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

      request_data = result["data"]["request"]
      expect(request_data["id"]).to be == request.id
      expect(request_data["organization"]["value"]["id"]).to be == ro.organization_id
      expect(request.reload.organization_id).to eq(ro.organization_id)
    end

    it "move to another category" do
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
          inspector_priority: "low",
          order_quantity: 1)
        orig_request = request.to_hash

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

        variables = {input: {id: request.id, category: new_category.id}}

        result = query(q, user.id, variables)

        expect(result).to be == {
          "data" => {
            "change_request_category" => {
              "id" => request.id,
              "category" => {
                "value" => {
                  "id" => new_category.id
                }
              }
            }
          }
        }

        request.reload

        # NOTE: admin effectivly has not "other" categories, inspection fields should NOT be reset!
        if user_name === "admin"
          expect(request.approved_quantity).to eq orig_request[:approved_quantity]
          expect(request.order_quantity).to eq orig_request[:order_quantity]
          expect(request.inspection_comment).to eq orig_request[:inspection_comment]
          expect(request.inspector_priority).to eq orig_request[:inspector_priority]
        else
          expect(request.approved_quantity).to be_nil
          expect(request.order_quantity).to be_nil
          expect(request.inspection_comment).to be_nil
          expect(request.inspector_priority).to eq("medium")
        end
      end
    end

    it "move to another budget period" do
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
        orig_request = request.to_hash

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

        variables = {input: {id: request.id, budget_period: new_budget_period.id}}

        result = query(q, user.id, variables)

        expect(result).to be == {
          "data" => {
            "change_request_budget_period" => {
              "id" => request.id,
              "budget_period" => {
                "value" => {
                  "id" => new_budget_period.id
                }
              }
            }
          }
        }

        request = request.reload

        expect(request.approved_quantity).to eq orig_request[:approved_quantity]
        expect(request.order_quantity).to eq orig_request[:order_quantity]
        expect(request.inspection_comment).to eq orig_request[:inspection_comment]
        expect(request.inspector_priority).to eq orig_request[:inspector_priority]
      end
    end
  end

  context "form editing flow" do
    # TODO: maybe add mutation examples here as well

    let(:q) do
      # from client/src/containers/RequestEdit.js
      <<-GRAPHQL
        query RequestForEdit($id: [ID!]!) {
          requests(id: $id) {
            ...RequestFieldsForEdit
            actionPermissions {
              delete
              edit
              moveBudgetPeriod
              moveCategory
            }
          }
          main_categories {
            id
            name
            categories {
              id
              name
            }
          }
          budget_periods(whereRequestsCanBeMovedTo: $id) {
            id
            name
          }
          settings {
            inspection_comments
          }
        }

        fragment RequestFieldsForEdit on Request {
          ...RequestFieldsForIndex
          template {
            value {
              id
              article_name
            }
          }
          user {
            read
            write
            required
            value {
              id
              firstname
              lastname
            }
          }
          article_name {
            ...RequestFieldString
          }
          model {
            read
            write
            required
            value {
              id
              product
              version
            }
          }
          supplier_name {
            ...RequestFieldString
          }
          supplier {
            read
            write
            required
            value {
              id
              name
            }
          }
          receiver {
            ...RequestFieldString
          }
          price_cents {
            ...RequestFieldInt
          }
          price_currency {
            ...RequestFieldString
          }
          requested_quantity {
            ...RequestFieldInt
          }
          approved_quantity {
            ...RequestFieldInt
          }
          order_quantity {
            ...RequestFieldInt
          }
          priority {
            read
            write
            required
            value
          }
          inspector_priority {
            read
            write
            required
            value
          }
          state
          article_number {
            ...RequestFieldString
          }
          motivation {
            ...RequestFieldString
          }
          replacement {
            ...RequestFieldBoolean
          }
          room {
            read
            write
            required
            value {
              id
              name
              building {
                id
                name
              }
            }
          }
          inspection_comment {
            ...RequestFieldString
          }
          attachments {
            read
            write
            required
            value {
              id
              filename
              url
            }
          }
          accounting_type {
            ...RequestFieldString
          }
          cost_center {
            read
            write
            value
          }
          procurement_account {
            read
            write
            value
          }
          general_ledger_account {
            read
            write
            value
          }
          internal_order_number {
            ...RequestFieldString
          }
        }

        fragment RequestFieldsForIndex on Request {
          id
          user {
            value {
              id
              firstname
              lastname
            }
          }
          category {
            read
            write
            required
            value {
              id
              name
              main_category {
                id
                name
              }
            }
          }
          budget_period {
            read
            write
            required
            value {
              name
              id
            }
          }
          article_name {
            value
          }
          model {
            value {
              id
              product
              version
            }
          }
          receiver {
            value
          }
          organization {
            value {
              id
              name
              shortname
              department {
                id
                name
              }
            }
          }
          price_cents {
            value
          }
          price_currency {
            value
          }
          requested_quantity {
            read
            value
          }
          approved_quantity {
            read
            value
          }
          order_quantity {
            read
            value
          }
          replacement {
            value
          }
          priority {
            value
          }
          state
          article_number {
            value
          }
          supplier {
            value {
              name
            }
          }
          supplier_name {
            value
          }
          receiver {
            value
          }
          room {
            value {
              id
              name
              building {
                id
                name
              }
            }
          }
          motivation {
            value
          }
          inspection_comment {
            value
          }
          inspector_priority {
            value
          }
          accounting_type {
            value
          }
          cost_center {
            value
          }
          general_ledger_account {
            value
          }
          procurement_account {
            value
          }
          internal_order_number {
            value
          }
        }

        fragment RequestFieldString on RequestFieldString {
          value
          read
          write
          required
        }

        fragment RequestFieldInt on RequestFieldInt {
          value
          read
          write
          required
        }

        fragment RequestFieldBoolean on RequestFieldBoolean {
          value
          read
          write
          required
        }
      GRAPHQL
    end

    let(:expected_request_data) do
      bp = BudgetPeriod.find(id: request[:budget_period_id])
      sc = Category.find(id: request[:category_id])
      mc = MainCategory.find(id: sc[:main_category_id])
      org = Organization.find(id: request[:organization_id])
      dep = Organization.find(id: org[:parent_id])
      room = Room.find(id: request.room_id)
      building = Building.find(id: room[:building_id])

      {
        id: request.id,
        category: {read: true,
                   write: true,
                   required: true,
                   value: sc.to_hash.slice(:id, :name).merge(main_category: mc.to_hash.slice(:id, :name))},
        budget_period: {read: true,
                        write: true,
                        required: true,
                        value: bp.to_hash.slice(:id, :name)},
        article_name: {value: request.article_name, read: true, write: true, required: true},
        model: {read: true, write: true, required: false, value: nil},
        receiver: {value: nil, read: true, write: true, required: false},
        organization: {value: org.to_hash.slice(:id, :name, :shortname).merge(department: dep.to_hash.slice(:id, :name))},
        price_cents: {value: request.price_cents, read: true, write: true, required: true},
        price_currency: {value: request.price_currency, read: true, write: false, required: true},
        requested_quantity: {value: request.requested_quantity, read: true, write: true, required: true},
        replacement: {value: request.replacement, read: true, write: true, required: true},
        priority: {read: true, write: true, required: true, value: request.priority.upcase},
        state: "NEW",
        article_number: {value: request.article_number, read: true, write: true, required: false},
        supplier: {read: true, write: true, required: false, value: nil},
        supplier_name: {value: request.supplier_name, read: true, write: true, required: false},
        room: {read: true,
               write: true,
               required: true,
               value: room.to_hash.slice(:id, :name).merge(building: building.to_hash.slice(:id, :name))},
        motivation: {value: request.motivation, read: true, write: true, required: true},
        attachments: {read: true, write: true, required: false, value: []},
        actionPermissions: {delete: true, edit: true, moveBudgetPeriod: true, moveCategory: true}
      }
    end

    context "requester" do
      let(:user) do
        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)
        requester
      end

      let(:expected_user_info) do
        {user: {read: true,
                write: false,
                required: true,
                value: user.to_hash.slice(:id, :firstname, :lastname)}}
      end

      let(:expected_accounting_info) do
        {
          cost_center: {read: false, value: nil, write: false},
          general_ledger_account: {read: false, value: nil, write: false},
          procurement_account: {read: false, write: false, value: nil}
        }
      end

      let(:expected_inspector_info) do
        {
          accounting_type: {value: nil, read: false, write: false, required: true},
          approved_quantity: {value: nil, read: false, write: false, required: false},
          internal_order_number: {value: nil, read: false, write: false, required: false},
          order_quantity: {value: nil, read: false, write: false, required: false},
          inspection_comment: {value: nil, read: false, write: false, required: false},
          inspector_priority: {read: false, write: false, required: true, value: nil}
        }
      end

      let(:expected_request_data_complete) do
        expected_request_data
          .merge(template: {value: nil})
          .merge(expected_accounting_info)
          .merge(expected_user_info)
          .merge(expected_inspector_info)
      end

      context "without template" do
        let(:request) do
          FactoryBot.create(:request, {
            user_id: user.id
          })
        end

        example "form data" do
          variables = {id: [request.id]}
          result = query(q, user.id, variables).deep_symbolize_keys

          expect(result[:data][:requests].length).to be 1
          expect(result[:data][:requests].first)
            .to eq(expected_request_data_complete)
        end
      end

      context "with template" do
        let(:template) do
          FactoryBot.create(:template)
        end

        let(:request) do
          FactoryBot.create(:request, {
            user_id: user.id,
            template_id: template.id
          })
        end

        example "form data" do
          variables = {id: [request.id]}
          result = query(q, user.id, variables).deep_symbolize_keys

          expected_data = expected_request_data_complete.merge(
            article_name: {read: true, write: false, required: true, value: template.article_name},
            article_number: {read: true, write: false, required: false, value: template.article_number},
            model: {read: true, write: false, required: false, value: Model.find(id: template.model_id)},
            price_cents: {read: true, write: false, required: true, value: template.price_cents},
            supplier: {read: true, write: false, required: false, value: Supplier.find(id: template.supplier_id)},
            supplier_name: {read: true, write: false, required: false, value: template.supplier_name},
            template: {value: {id: template.id, article_name: template.article_name}}
          )

          expect(result[:data][:requests].length).to be 1
          expect(result[:data][:requests].first).to eq(expected_data)
        end
      end
    end

    context "without template & inspector" do
      let(:category) do
        FactoryBot.create(:category)
      end

      let(:user) do
        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)
        FactoryBot.create(:category_inspector,
          category_id: category.id,
          user_id: requester.id)
        requester
      end

      let(:request) do
        FactoryBot.create(:request, {
          user_id: user.id,
          category_id: category.id
        })
      end

      let(:expected_accounting_info) do
        {
          cost_center: {read: true, value: category.cost_center, write: false},
          general_ledger_account: {read: true, value: category.general_ledger_account, write: false},
          procurement_account: {read: true, write: false, value: category.procurement_account}
        }
      end

      let(:expected_user_info) do
        {user: {read: true,
                write: true,
                required: true,
                value: user.to_hash.slice(:id, :firstname, :lastname)}}
      end

      let(:expected_inspector_info) do
        {
          accounting_type: {value: request.accounting_type, read: true, write: true, required: true},
          approved_quantity: {value: request.approved_quantity, read: true, write: true, required: false},
          internal_order_number: {value: request.internal_order_number, read: true, write: true, required: false},
          order_quantity: {value: request.order_quantity, read: true, write: true, required: false},
          inspection_comment: {value: request.inspection_comment, read: true, write: true, required: false},
          inspector_priority: {read: true, write: true, required: true, value: request.inspector_priority.upcase}
        }
      end

      example "form data" do
        variables = {id: [request.id]}
        result = query(q, user.id, variables).deep_symbolize_keys

        expect(result[:data][:requests].length).to be 1
        expect(result[:data][:requests].first)
          .to eq(
            expected_request_data
            .merge(template: {value: nil})
            .merge(expected_accounting_info)
            .merge(expected_user_info)
            .merge(expected_inspector_info)
          )
      end
    end
  end
end
