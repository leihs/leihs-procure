# frozen_string_literal: true

feature 'Archive Template(s)' do
  before(:each) do
    @requester = FactoryBot.create(:user)
    @budget_period = FactoryBot.create(:procurement_budget_period, :inspection_phase)
    @category = FactoryBot.create(:procurement_category)
    FactoryBot.create(:procurement_inspector, user: @requester, category: @category)
    FactoryBot.create(:procurement_requester, user: @requester)

    @templates = Array(1..5).map do |_|
      FactoryBot.create(:procurement_template, :archiveable, category: @category)
    end

    @requests = Array(1..5).map.with_index do |_element, index|
      template_object = @templates[index]
      FactoryBot.create(:procurement_request, category: @category, template_id: template_object.id, user: @requester,
                                              budget_period: @budget_period)
    end
  end

  context 'as requester' do
    before(:each) do
      Helpers::User.sign_in_as @requester
      visit('/templates/edit')
    end

    scenario 'archive single template' do
      archive_buttons = all('label[id^="btn_archive"]')
      archive_buttons.first.click
      find('button[type="submit"]').click
    end

    scenario 'archive multiple templates' do
      archive_buttons = all('label[id^="btn_archive"]')
      archive_buttons.each(&:click)
      find('button[type="submit"]').click
    end
  end
end
