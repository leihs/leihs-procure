require 'spec_helper'
require 'pry'


feature 'Manage rooms', type: :feature do

  context ' an admin' do

    before :each do
      @admin = FactoryBot.create :admin
      @rooms = 10.times.map { FactoryBot.create :room }
      sign_in_as @admin
    end

    scenario 'deleting a room' do

      visit '/admin/'
      click_on 'Rooms'

      @rooms.each { |room| expect(page).to have_content room.name }

      click_on @rooms.first.name
      @room_path = current_path

      click_on 'Delete' # delete page
      click_on 'Delete' # submit / confirm

      wait_until { current_path ==  "/admin/rooms/" }

      @rooms.drop(1).each { |room| expect(page).to have_content room.name }

      expect(page).not_to have_content @rooms.first.name

    end

    scenario 'deleting a general room is forbidden' do

      room = Room.where(general: true).first
      @room_path = "/admin/rooms/#{room.id}"
      visit @room_path

      click_on 'Delete' # delete page
      click_on 'Delete' # submit / confirm

      expect(page).to have_content "ERROR 403"

      expect(current_path).to eq "#{@room_path}/delete"

      visit '/admin/'
      click_on 'Rooms'

      expect(page).to have_content room.name

    end
  end

end
