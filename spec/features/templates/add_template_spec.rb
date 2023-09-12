# frozen_string_literal: true

feature 'Add Template to Category' do
  before(:each) do
    @user = FactoryBot.create(:user)
    @category = FactoryBot.create(:procurement_category)
    FactoryBot.create(:procurement_inspector, user: @user, category: @category)

    visit('/templates/edit')
    fill_in('inputEmail', with: @user.email)
    # find(@user.email).send_keys(:enter)
    find_button('Continue').click

    fill_in('inputPassword', with: 'password')
    find_button('Continue').click
  end

  context 'is logged in' do
    scenario 'insert data into form' do
      # find add button and click it
      find('.m-0.p-0.btn.btn-link').click
      tbody = find('tbody')

      # Iterate over each table row
      tbody.all('tr').each_with_index do |row, _row_index|
        # Locate all table data (<td>) elements within the row
        tds = row.all('td')

        # Iterate over the table data elements
        tds.each_with_index do |td, column_index|
          # Replace 'input' with the appropriate input element selector
          # input = td.find('input') # Use visible: false to locate hidden inputs
          begin
            input = td.find('input', visible: :all)
          rescue Capybara::ElementNotFound
            # Handle the case when no <input> element is found
            input = nil # or any other appropriate action
          end
          # Check if an input field exists within the <td> element
          next unless input

          case column_index
          when 1
            input.set(Faker::Device.model_name)
          when 2
            input.set(Faker::Device.model_name)
          when 3
            input.set(Faker::Device.model_name)
          when 4
            input.set(Faker::Device.model_name)
          end
        end
      end
      find('button[type="submit"]').click
      binding.pry
    end
  end
end
