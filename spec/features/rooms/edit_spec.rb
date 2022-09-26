require 'spec_helper'
require 'pry'

feature 'Manage rooms', type: :feature do

  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }

  before :each do
    @admin = FactoryBot.create :admin
    @rooms = 10.times.map { FactoryBot.create(:room) }
    @room = @rooms.sample
    @building = @rooms.map(&:building).detect { |b| b.id != @room.building_id }
  end

  context "an admin via the UI" do
    before(:each){ sign_in_as @admin }

    scenario 'edits a room' do
      visit '/admin/'
      click_on 'Rooms'
      click_on @room.name
      @room_path = current_path

      click_on 'Edit'
      fill_in 'name', with: name
      fill_in 'description', with: description
      select(@building.name, from: 'Building')
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until {current_path == @room_path}
      wait_until { all(".wait-component").empty? }

      input_values = all("input").map(&:value).join(" ")
      input_values += all("select").map(&:value).join(" ")
      expect(page.text + input_values).to have_content name
      expect(page.text + input_values).to have_content description
      expect(page.text + input_values).to have_content @building.name

      within find(".nav-component nav", match: :first) do
        click_on "Rooms"
      end
      wait_until { current_path ==  "/admin/rooms/" }
      expect(page).to have_content name
    end

  end

end
