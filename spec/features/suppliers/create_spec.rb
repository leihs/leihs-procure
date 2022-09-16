require 'spec_helper'
require 'pry'

feature 'Manage suppliers', type: :feature do

  before :each do
    @admin = FactoryBot.create :admin
  end

  let(:name) { Faker::Company.name}
  let(:note) { Faker::Markdown.sandwich }

  context 'an admin via the UI' do

    before(:each){ sign_in_as @admin }

    scenario ' creates a new supplier ' do

      visit '/admin/'
      click_on 'Suppliers'
      expect(all("a, button", text: 'Create')).not_to be_empty
      click_on 'Create'
      fill_in 'name', with: name
      fill_in 'note', with: note
      click_on 'Create'
      wait_until { all(".modal").empty? }
      wait_until { not page.has_content? "Create Supplier" }
      @supplier_path = current_path
      @inventory_pool_id = current_path.match(/.*\/([^\/]+)/)[1]
      input_values = all("input").map(&:value).join(" ")
      expect(page.text + input_values).to have_content name
      expect(page.text + input_values).to have_content note

      # The inventory pools path includes the newly created inventory pool and
      # we can get to it via clicking its name
      within find(".nav-component nav", match: :first) do
        click_on "Suppliers"
      end
      wait_until { current_path == "/admin/suppliers/" }
      wait_until { page.has_content? name }
      click_on name
      wait_until { current_path == @supplier_path }
      expect(page).to have_content "No Items"

    end

  end

end
