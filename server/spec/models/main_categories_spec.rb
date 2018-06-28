require 'spec_helper'

describe 'main category' do
  context 'delete' do
    context 'error' do
      it 'if child category has requests' do
        main_cat = FactoryBot.create(:main_category)
        cat = FactoryBot.create(:category, main_category_id: main_cat.id)
        FactoryBot.create(:request, category_id: cat.id)
        expect { main_cat.destroy }
          .to raise_error Sequel::ForeignKeyConstraintViolation
      end

      it 'if child category has requests' do
        main_cat = FactoryBot.create(:main_category)
        cat = FactoryBot.create(:category, main_category_id: main_cat.id)
        FactoryBot.create(:template, category_id: cat.id)
        expect { main_cat.destroy }
          .to raise_error Sequel::ForeignKeyConstraintViolation
      end
    end
  end
end
