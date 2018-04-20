require 'spec_helper'
require 'pry'

feature 'Manage delegations', type: :feature do

  context 'an admin user and a bunch of other users and a bunch of delegations' do

    let :sign_in_as_admin do
      visit '/'
      click_on 'Sign in with password'
      fill_in 'email', with: @admin.email
      fill_in 'password', with: @admin.password
      click_on 'Sign in'
    end


    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      @delegations = 15.times.map do
        FactoryBot.create :delegation
      end

      sign_in_as_admin
    end


    scenario 'adding a new delegation ' do 

      visit '/admin/'
      click_on 'Delegations'
      click_on 'Add delegation'
      fill_in 'name', with: 'Zuckerberg'

      click_on 'Choose responsible user'

      #there should be many to coose from 
      wait_until do 
        all('a', text: 'Choose as responsible user').count > 1
      end

      @delegator = @users.first

      # test filtering by term 
      fill_in 'term', with: "#{@delegator.firstname} #{@delegator.lastname}"

      wait_until do 
        all('a', text: 'Choose as responsible user').count == 1
      end
      click_on 'Choose as responsible user'
      click_on 'Create'

      # the delegator has been added to the users of the delegation by default
      click_on 'Users'
      expect(page).to have_content '1 User in Delegation Zuckerberg'
      expect(page).to have_content @delegator.firstname
      expect(page).to have_content @delegator.lastname
      expect(page).to have_content 'Remove'

      # the delegation can be found by filtering
      click_on_first ('Delegations')
      fill_in 'term', with: 'Zuckerberg'
      wait_until do
        all(".delegation").count == 1
      end
      find(".delegation", text: 'Zuckerberg')

    end

  end

end
