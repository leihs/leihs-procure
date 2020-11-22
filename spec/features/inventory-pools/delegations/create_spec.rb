require 'spec_helper'
require 'pry'

shared_examples :create do
  scenario 'creating a delegation works' do
    click_on 'Inventory-Pools'
    click_on @pool.name
    click_on 'Delegations'
    click_on 'Create'
    expect(find_field('protected')).to be_checked
    fill_in :name , with: 'Foo-Delegation'
    uncheck :protected
    click_on 'Choose responsible user'
    resp_user = @users.sample
    within('tr', text: resp_user.email) do
      click_on 'Choose user'
    end
    expect(find_field('name').value).to eq 'Foo-Delegation'
    expect(find_field('protected')).not_to be_checked
    check :protected
    click_on 'Choose responsible user'
    resp_user = @users.sample
    within('tr', text: resp_user.email) do
      click_on 'Choose user'
    end
    expect(find_field('protected')).to be_checked
    click_on 'Create'
    wait_until do
      current_path.match? %{/admin/inventory-pools/[^/]+/delegations/[^/]+}
    end
    expect(page).to have_content 'Foo-Delegation'
    expect(page).to have_content resp_user.email
    expect(find('tr', text: 'Protected').text).to have_content 'yes'
    click_on 'Users'
    expect(page).to have_content resp_user.email
  end
end

feature 'Creating a delegation' do

  before :each do
    @admin = FactoryBot.create :admin
    @users = 10.times.map{ FactoryBot.create :user }
  end

  context 'a inventory pool' do
    before(:each) {@pool = FactoryBot.create :inventory_pool}

    context 'as an admin' do
      before(:each){ @user = @admin }
      context 'via the UI' do
        before(:each){sign_in_as @user}
        include_examples :create
      end
    end

    context 'as a lending_manager of the pool ' do
      before :each do
        @user = FactoryBot.create :user
        FactoryBot.create :access_right, user: @user,
          inventory_pool: @pool, role: 'lending_manager'
      end
      context 'via the UI' do
        before(:each) { sign_in_as @user}
        include_examples :create
      end
    end
  end
end