require 'spec_helper'
require 'pry'

feature 'Filter users', type: :feature do

  context 'an admin user and a bunch of users' do

    before :each do
      @admin = FactoryBot.create :admin


      @enabeld_user = FactoryBot.create :user, account_enabled: true
      @not_enabeld_user = FactoryBot.create :user, account_enabled: false

      sign_in_as @admin
    end


    describe 'account enabled filters' do

      before :each do
        visit '/admin/'
        click_on 'Users'
        wait_until { not page.has_content? "Please wait" }
      end

      scenario 'cycle through account enabled filters' do

        select('yes', from: 'Account enabled')
        wait_until { all("table.users tbody tr").count == 2 }
        expect(page).to have_content  @enabeld_user.lastname
        expect(page).not_to have_content  @not_enabeld_user.lastname
        expect(page).to have_select('Account enabled', selected: 'yes')

        select('any', from: 'Account enabled')
        wait_until { all("table.users tbody tr").count == 3 }
        expect(page).to have_select('Account enabled', selected: 'any')

        select('no', from: 'Account enabled')
        wait_until { all("table.users tbody tr").count == 1 }
        expect(page).not_to have_content  @enabeld_user.lastname
        expect(page).to have_content  @not_enabeld_user.lastname
        expect(page).to have_select('Account enabled', selected: 'no')

        select('any', from: 'Account enabled')
        wait_until { all("table.users tbody tr").count == 3 }
        expect(page).to have_select('Account enabled', selected: 'any')

      end

    end

  end
end
