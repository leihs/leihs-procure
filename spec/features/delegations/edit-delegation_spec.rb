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


    scenario 'edit a delegation ' do 

      visit '/admin/'
      click_on 'Delegations'

      # pick a random delegation
      delegation = @delegations.sample

      fill_in 'term', with: delegation.firstname
      click_on delegation.firstname

      click_on 'Edit'

      fill_in 'name', with: 'Zuckerberg'

      click_on 'Choose responsible user'

      new_responsible_user = @users.to_a.sample

      fill_in 'term', with: "#{new_responsible_user.firstname} #{new_responsible_user.lastname}"

      wait_until do 
        all('a', text: 'Choose as responsible user').count == 1
      end

      click_on 'Choose as responsible user'

      click_on 'Save'

      # wait until we are back on the delegation page with the new name
      wait_until do
        page.has_content? "Delegation Zuckerberg"
      end

      # the new responsible user is now visible too

      wait_until do
        page.has_content? new_responsible_user.firstname
        page.has_content? new_responsible_user.lastname
      end

    end

  end

end
