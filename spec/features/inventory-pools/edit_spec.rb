require 'spec_helper'
require 'pry'

feature 'Manage inventory-pools', type: :feature do

  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }
  let(:shortname) { Faker::Name.initials }
  let(:email) { Faker::Internet.email }

  context 'an admin and several pools ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pools = 10.times.map { FactoryBot.create :inventory_pool }
      @pool = @pools.sample
    end

    context "an admin via the UI" do
      before(:each){ sign_in_as @admin }

      scenario 'edits an inventory pool' do
        visit '/admin/'
        click_on 'Inventory-Pools'
        click_on @pool.name
        @inventory_pool_path = current_path
        click_on 'Edit'
        fill_in 'name', with: name
        fill_in 'description', with: description
        fill_in 'shortname', with: shortname
        fill_in 'email', with: email
        uncheck 'is_active'
        click_on 'Save'
        wait_until { all(".modal").empty? }
        wait_until {current_path == @inventory_pool_path}
        wait_until { all(".wait-component").empty? }

        input_values = all("input").map(&:value).join(" ")
        expect(page.text + input_values).to have_content name
        expect(page.text + input_values).to have_content shortname
        expect(page.text + input_values).to have_content email
        expect(page.text + input_values).to have_content description
        click_on 'Inventory-Pools'
        wait_until { current_path ==  "/admin/inventory-pools/" }
        expect(page).to have_content name
      end

    end

    context 'a inventory-pool manager' do

      before :each do
        @manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @manager,
          inventory_pool: @pool, role: 'inventory_manager'
      end

      context 'via the UI' do
        before(:each){ sign_in_as @manager }
        scenario 'edits the pool' do
          visit '/admin/'
          click_on 'Inventory-Pools'
          click_on @pool.name
          @inventory_pool_path = current_path
          click_on 'Edit'
          fill_in 'name', with: name
          click_on 'Save'
          wait_until {current_path == @inventory_pool_path}
          expect(page).to have_content name
        end

      end

      context 'a lending manager' do

        before :each do
          @manager = FactoryBot.create :user
          FactoryBot.create :access_right, user: @manager,
            inventory_pool: @pool, role: 'lending_manager'
        end

        context 'via the UI' do
          before(:each){ sign_in_as @manager }
          scenario 'edits the pool' do
            visit '/admin/'
            click_on 'Inventory-Pools'
            click_on @pool.name
            expect(all("a, button", text: 'Edit')).to be_empty
          end

        end


        context 'via API' do

          let :http_client do
            plain_faraday_client
          end

          let :prepare_http_client do
            @api_token = FactoryBot.create :api_token, user_id: @manager.id
            @token_secret = @api_token.token_secret
            http_client.headers["Authorization"] = "Token #{@token_secret}"
            http_client.headers["Content-Type"] = "application/json"
          end

          before :each do
            prepare_http_client
          end

          scenario 'editing the pool is forbidden' do
            resp = http_client.patch "/admin/inventory-pools/#{@pool[:id]}",
              {name: "New Name"}.to_json
            expect(resp.status).to be== 403
          end

        end

      end

    end

  end

end
