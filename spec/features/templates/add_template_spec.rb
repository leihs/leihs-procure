# frozen_string_literal: true

# require_relative '../../helpers/users'

feature 'Add Template to Category' do
  before(:each) do
    @inspector = FactoryBot.create(:user)
    @category = FactoryBot.create(:procurement_category)
    FactoryBot.create(:procurement_inspector, user: @inspector, category: @category)
  end

  context 'as inspector' do
    before(:each) do
      Helpers::User.sign_in_as @inspector
      click_on('Vorlagen')
    end

    scenario 'insert data into form' do
      # find add button and click it
      find('.m-0.p-0.btn.btn-link').click
      tbody = find('tbody')

      # Iterate over each table row
      tbody.all('tr').each_with_index do |row, _row_index|
        # Locate all table data (<td>) elements within the row
        tds = row.all('td')

        # Replace 'input' with the appropriate input element selector
        tds[1].find('input').set Faker::Device.model_name
        tds[2].find('input').set Faker::Device.model_name
        tds[3].find('input').set Faker::Commerce.price
        tds[4].find('input').set Faker::Camera.brand
      end
      find('button[type="submit"]').click
    end
  end
end
