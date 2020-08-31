require 'spec_helper'
require 'pry'

feature 'Manage direct delegation users ', type: :feature do

  context 'an admin user and a bunch of other users, each in 3 groups, and one delegation' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_a

      @groups = 3.times.map do
        FactoryBot.create :group
      end.to_a

      @users.each do |user|
        @groups.shuffle.take(2).each do |group|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end

      @delegation = FactoryBot.create :delegation

      sign_in_as @admin
    end

    scenario 'adding and removing direct users' do

      visit '/admin/'
      click_on 'Delegations'
      click_on @delegation.firstname
      click_on 'Users'


      select('members and non-members', from: 'Membership')
      wait_until { all("tbody.users tr.user").count > 0}


      [1, 2, 3].each do |count|
        within_first("td.direct-member", text: 'add'){ click_on 'add' }
        wait_until { all("td.direct-member", text: 'remove').count == count}
      end

      [2, 1, 0].each do |count|
        within_first("td.direct-member", text: 'remove'){ click_on 'remove' }
        wait_until { all("td.direct-member", text: 'remove').count == count}
      end


      # now do it again on the second page, internally there is some per indexing
      # working behind the scenes which is not too hard to break in the code

      select('12', from: 'Per page')
      click_on_first 'next'
      wait_until { all("tbody.users tr.user").count > 0}

      [1, 2, 3].each do |count|
        within_first("td.direct-member", text: 'add'){ click_on 'add' }
        wait_until { all("td.direct-member", text: 'remove').count == count}
      end

      [2, 1, 0].each do |count|
        within_first("td.direct-member", text: 'remove'){ click_on 'remove' }
        wait_until { all("td.direct-member", text: 'remove').count == count}
      end

    end


    scenario 'adding and removing users via groups' do

      user = @users.sample

      visit '/admin/'
      click_on 'Delegations'
      click_on @delegation.firstname
      delegation_path = current_path
      click_on 'Users'
      select('members and non-members', from: 'Membership')
      fill_in 'users-search-term', with: user.email
      wait_until { all("table.users tbody tr").count == 1 }
      within(".group-member"){click_on "add"}
      wait_until { page.has_content? "Groups of Entitlement-Group" }
      expect(page).not_to have_content "remove"
      within(first(".groups .member")){click_on "add"}
      expect(page).to have_content "remove"

      # verify there are now members in this delegation on the users page
      visit delegation_path
      click_on 'Users'
      expect(page).to have_select('Membership', selected: 'members')
      expect( all("table.users tbody tr").count ).to be> 1

      # remove the group via groups
      visit delegation_path
      click_on 'Groups'
      expect(page).to have_select('Membership', selected: 'members')
      expect( all("table.groups tbody tr").count ).to be== 1
      click_on 'remove'
      wait_until{ page.has_content? "No (more) groups found." }

      # verify there are no members in this delegation on the users page
      visit delegation_path
      click_on 'Users'
      expect(page).to have_select('Membership', selected: 'members')
      wait_until{ page.has_content? "No (more) users found." }

    end

  end

end
