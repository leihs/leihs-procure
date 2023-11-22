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

          # within '.modal' do
          #   within("tr", text: "de-CH") do
          #     expect(find_field('active', disabled: true)).to be_checked
          #     expect(find_field('default', disabled: true)).not_to be_checked
          #   end
          # end
          #

          # within("tr", text: "en-GB") do
          #   expect(find_field('en-GB-active', disabled: true)).to be_checked
          #   expect(find_field('en-GB-default', disabled: true)).to be_checked
          # end

          # The commented approaches above are not working because the element is invisible
          expect(page.execute_script("return document.getElementById('de-CH-active').hasAttribute('checked');")).to be_truthy
          expect(page.execute_script("return document.getElementById('de-CH-default').hasAttribute('checked');")).to be_falsey

          expect(page.execute_script("return document.getElementById('en-GB-active').hasAttribute('checked');")).to be_truthy
          expect(page.execute_script("return document.getElementById('en-GB-default').hasAttribute('checked');")).to be_truthy

          click_on "Edit"
          within '.modal' do
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
          end

          # within("tr", text: "de-CH") do
          #   expect(find_field('active', disabled: true)).to be_checked
          #   expect(find_field('default', disabled: true)).to be_checked
          # end
          # within("tr", text: "en-GB") do
          #   expect(find_field('active', disabled: true)).not_to be_checked
          #   expect(find_field('default', disabled: true)).not_to be_checked
          # end
          expect(page.execute_script("return document.getElementById('de-CH-active').hasAttribute('checked');")).to be_truthy
          expect(page.execute_script("return document.getElementById('de-CH-default').hasAttribute('checked');")).to be_truthy

          expect(page.execute_script("return document.getElementById('en-GB-active').hasAttribute('checked');")).to be_falsey
          expect(page.execute_script("return document.getElementById('en-GB-default').hasAttribute('checked');")).to be_falsey
        end
      end
    end
  end
end
