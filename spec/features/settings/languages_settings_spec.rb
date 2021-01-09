require 'spec_helper'
require 'pry'

feature 'SMTP-Settings' do

  context 'a plain admin exist' do
    before :each do
      @plain_admin = FactoryBot.create :admin
    end

    context 'a plain_admin' do
      before(:each){@user = @plain_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'updates the Miscellaneous-Settings' do
          click_on "Settings"
          click_on "Languages"
          wait_until { page.has_content? "de-CH" }
          within("tr", text: "de-CH") do
            expect(find_field('active', disabled: true)).to be_checked
            expect(find_field('default', disabled: true)).not_to be_checked
          end
          within("tr", text: "en-GB") do
            expect(find_field('active', disabled: true)).to be_checked
            expect(find_field('default', disabled: true)).to be_checked
          end
          click_on "Edit"
          within("tr", text: "en-GB") do
            # still disabled !
            expect(find_field('active', disabled: true)).to be_checked
            expect(find_field('default', disabled: true)).to be_checked
          end
          within("tr", text: "de-CH") do
            check "default"
          end
          within("tr", text: "en-GB") do
            uncheck "active"
          end
          click_on "Save"
          within("tr", text: "de-CH") do
            expect(find_field('active', disabled: true)).to be_checked
            expect(find_field('default', disabled: true)).to be_checked
          end
          within("tr", text: "en-GB") do
            expect(find_field('active', disabled: true)).not_to be_checked
            expect(find_field('default', disabled: true)).not_to be_checked
          end

        end
      end
    end
  end
end
