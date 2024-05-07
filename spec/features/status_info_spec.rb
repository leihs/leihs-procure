require 'spec_helper'
require 'pry'

feature 'Status-info', skip: "Information not gathered properly. See `leihs.core.status` for more details." do
  context 'a system-admin exists' do
    before(:each){ @admin = FactoryBot.create :system_admin }
    context 'system_admin via the UI' do
      before(:each){ sign_in_as @admin  }
      scenario 'Visiting the Status-Info page' do
        click_on 'Status-Info'
        wait_until{current_path == '/admin/status'}
        wait_until{ page.has_content? 'memory' }
        wait_until{ page.has_content? 'db-pool' }
      end
    end
  end
end
