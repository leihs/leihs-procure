require 'spec_helper'

describe 'budget period' do
  context 'delete' do
    context 'error' do
      it 'if has requests' do
        bp = FactoryBot.create(:budget_period)
        FactoryBot.create(:request, budget_period_id: bp.id)
        expect { bp.destroy }
          .to raise_error Sequel::ForeignKeyConstraintViolation
      end
    end
  end
end
