require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an existing admin user ' do

    before :each do
      @user = FactoryBot.create :admin
    end


    scenario 'setting, seeing and then removing the user image works' do

      sign_in_as @user

      click_on "Users"
      within '.users' do
        click_on_first @user.lastname
      end
      click_on 'Edit'

      find('label', text: 'Choose file').find('input') \
        .set(File.absolute_path('./spec/data/anon.jpg'))

      wait_until {page.has_content? 'Remove image'}

      click_on 'Save'

      wait_until do
        first('img.user-image-256') and \
          first('img.user-image-256')['src'][0..22] == 'data:image/jpeg;base64,'
      end

      click_on 'Edit'

      click_on 'Remove image'

      click_on 'Save'

      wait_until do
        first('img.user-image-256') and \
          first('img.user-image-256')['src'][0..31] == "https://www.gravatar.com/avatar/"
      end

    end

  end

end




