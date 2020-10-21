require 'spec_helper'
require 'pry'

feature 'Filter users', type: :feature do

  context 'an admin user and a bunch of users' do

    before :each do
      @admin = FactoryBot.create :admin
      @user = FactoryBot.create :user
      sign_in_as @admin
    end


    describe 'is admin filters' do

      before :each do
        visit '/admin/'
        click_on 'Users'
        wait_until { not page.has_content? "Please wait" }
      end

      scenario 'cycle through is admin filters' do

        select('yes', from: 'Is admin')
        wait_until { all("table.users tbody tr").count == 1 }
        expect(page).to have_content @admin.lastname
        expect(page).not_to have_content @user.lastname
        expect(page).to have_select('Is admin', selected: 'yes')

        select('any', from: 'Is admin')
        wait_until { all("table.users tbody tr").count == 2 }
        expect(page).to have_select('Is admin', selected: 'any')

        select('no', from: 'Is admin')
        wait_until { all("table.users tbody tr").count == 1 }
        expect(page).not_to have_content @admin.lastname
        expect(page).to have_content @user.lastname
        expect(page).to have_select('Is admin', selected: 'no')

        select('any', from: 'Is admin')
        wait_until { all("table.users tbody tr").count == 2 }
        expect(page).to have_select('Is admin', selected: 'any')

      end

    end

  end
end
