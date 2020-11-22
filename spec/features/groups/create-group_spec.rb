require 'spec_helper'
require 'pry'

feature 'Creating groups', type: :feature do

  context 'a bunch of groups, users and some admins exist;' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @users = 15.times.map { FactoryBot.create :user }
      @groups = 15.times.map { FactoryBot.create :group }
    end

    context "an admin" do

      before :each do
        @admin = @admins.sample
        sign_in_as @admin
      end

      scenario 'creates a new group ' do

        description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc.
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
        TEXT
        name = "La Grupa"

        visit '/admin/'
        click_on 'Groups'
        click_on 'Create group'
        fill_in 'name', with: name
        fill_in 'description', with: description
        check 'protected'

        click_on 'Create'
        wait_until{ all(".modal").empty? }
        click_on_first ('Groups')
        fill_in 'term', with: name
        wait_until { all(".group").count == 1 }

        # we can open the group by clicking on the name
        click_on name
        expect(page).to have_content "Group #{name}"

        # we can see the full description here too
        expect(page.text.tr("\n\r\s"," ")).to have_content description.tr("\n\r\s"," ")

      end

    end


    context "some inventory-pool's lending-manager " do

      before :each do
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'
      end

      context 'via the UI' do

        before :each do
          sign_in_as @lending_manager
        end

        scenario 'creates a new group ' do


          description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc.
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
          TEXT
          name = "La Grupa"

          visit '/admin/'
          click_on 'Groups'
          click_on 'Create group'
          fill_in 'name', with: name
          fill_in 'description', with: description
          expect(find(:checkbox, id: 'protected', disabled: true)).not_to be_checked
          click_on 'Create'

          click_on_first ('Groups')
          fill_in 'term', with: name
          wait_until { all(".group").count == 1 }
          click_on name
          expect(page).to have_content "Group #{name}"

          # we can see the full description here too
          expect(page.text.tr("\n\r\s"," ")).to have_content description.tr("\n\r\s"," ")

        end

      end

      context 'via the API' do

        let :http_client do
          plain_faraday_client
        end

        let :prepare_http_client do
          @api_token = FactoryBot.create :api_token, user_id: @lending_manager.id

          @token_secret = @api_token.token_secret
          http_client.headers["Authorization"] = "Token #{@token_secret}"
          http_client.headers["Content-Type"] = "application/json"
        end

        before :each do
          prepare_http_client
        end

        scenario "the protected property can not be set" do

          resp = http_client.post "/admin/groups/",
            {name: "New Group", org_id: '123', protected: true }.to_json
          expect(resp.status).to be== 403
          new_group = Group.where(org_id: '123').first
          expect(new_group).not_to be

        end

      end

    end

  end

end
