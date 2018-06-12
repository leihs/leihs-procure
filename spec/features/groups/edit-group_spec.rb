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


    scenario 'edit a group ' do 

      visit '/admin/'
      click_on 'Groups'


      description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc. 
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
        TEXT
      name = "La Grupa"


      # pick a random group
      group = @groups.sample

      fill_in 'term', with: group.name
      click_on group.name

      click_on 'Edit'

      fill_in 'name', with: name
      fill_in 'description', with: description

      click_on 'Save'

      # wait until we are back on the group page with the new name
      wait_until do
        page.has_content? "Group #{name}"
      end

      # the description is also set properly
      expect(page).to have_content description

    end

  end

end
