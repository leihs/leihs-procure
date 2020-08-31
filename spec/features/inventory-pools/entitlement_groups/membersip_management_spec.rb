require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, an entitlement-group, several users, each within at least one group' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @entitlement_group = FactoryBot.create :entitlement_group, inventory_pool_id: @pool.id
      @users = 100.times.map{ FactoryBot.create :user }
      @groups = 3.times.map { FactoryBot.create :group }
      @users.each do |user|
        @groups.shuffle.take(2).each do |group|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end
      sign_in_as @admin
    end

    scenario 'manage membership via direct membership' do
      click_on 'Inventory-Pools'
      click_on @pool.name
      click_on 'Entitlement-Groups'
      click_on @entitlement_group.name
      click_on 'Entitlement-Group Users'
      select 'members and non-members', from: 'Membership'
      click_on_first 'next' # go to the second page because we want also test some internal indexing complexity
      within(first 'tr.user') do
          expect(find_field("_member", disabled: true)).not_to be_checked
          within("td.direct-member") { click_on 'add' }
          wait_until { find_field("_member", disabled: true).checked? }
          click_on 'remove'
          wait_until { not find_field("_member", disabled: true).checked? }
      end
    end

    scenario 'manage membership via group membership' do
      user = @users.sample
      click_on 'Inventory-Pools'
      click_on @pool.name
      click_on 'Entitlement-Groups'
      click_on @entitlement_group.name
      click_on 'Entitlement-Group Users'
      select 'members and non-members', from: 'Membership'
      fill_in 'users-search-term', with: user.email
      wait_until{ all('tr.user').count == 1 }
      user_on_users_page = current_url
      within('tr.user') do
        expect(find_field("_member", disabled: true)).not_to be_checked
        within('td.group-member') {click_on 'add'}
      end
      click_on_first 'add'
      visit user_on_users_page
      # now the users is a member, wait_until because we might see stale cached data briefly
      wait_until { find_field('_member', disabled: true).checked? }
      within('td.group-member') { click_on 'edit' }
      click_on 'remove'
      visit user_on_users_page
      # now the users is no member anymore
      wait_until { not find_field('_member', disabled: true).checked? }
    end

  end
end

