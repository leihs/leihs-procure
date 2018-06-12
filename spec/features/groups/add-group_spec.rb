require 'spec_helper'
require 'pry'

feature 'Manage groups', type: :feature do

  context 'an admin user and a bunch of other users and a bunch of groups' do

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

      @groups = 15.times.map do
        FactoryBot.create :group
      end

      sign_in_as_admin
    end


    scenario 'adding a new group ' do 

      description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc. 
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
        TEXT
      name = "La Grupa"

      visit '/admin/'
      click_on 'Groups'
      click_on 'Add group'
      fill_in 'name', with: name
      fill_in 'description', with: description

      click_on 'Add'

      sleep 1 # strange, we get the misterious stale nodes problem without this (off and on!)

      # the group can be found by filtering
      click_on_first ('Groups')
      fill_in 'term', with: name
      wait_until do
        all(".group").count == 1
      end

      # we can open the group by clicking on the name 
      click_on name
      expect(page).to have_content "Group #{name}"

      # we can see the full description here too
      expect(page).to have_content description

    end

  end

end
