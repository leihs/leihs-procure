require 'spec_helper'
require 'pry'

feature 'Filter users by admin status ', type: :feature do

  context 'bunch of users exist, as an admin via the UI' do

    before :each do
      @admin = FactoryBot.create :admin
      @user = FactoryBot.create :user
      sign_in_as @admin
    end


    describe 'is_admin filter' do

      before :each do
        visit '/admin/'
        click_on 'Users'
        wait_until { not page.has_content? "Please wait" }
      end

      scenario 'cycle through is admin filters' do

        select('yes', from: 'Admin')
        wait_until { all("table.users tbody tr").count == 1 }
        expect(page).to have_content @admin.lastname
        expect(page).not_to have_content @user.lastname
        expect(page).to have_select('Admin', selected: 'yes')

        select('(any value)', from: 'Admin')
        wait_until { all("table.users tbody tr").count == 2 }
        expect(page).to have_select('Admin', selected: '(any value)')

        select('no', from: 'Admin')
        wait_until { all("table.users tbody tr").count == 1 }
        expect(page).not_to have_content @admin.lastname
        expect(page).to have_content @user.lastname
        expect(page).to have_select('Admin', selected: 'no')

        select('(any value)', from: 'Admin')
        wait_until { all("table.users tbody tr").count == 2 }
        expect(page).to have_select('Admin', selected: '(any value)')

      end

    end

  end
end
