require 'spec_helper'

describe 'short id' do
  it 'works' do
    # create budget period => create counter
    bp = FactoryBot.create(:budget_period, :requesting_phase, name: "XYZ")
    rc = RequestCounter.find(prefix: "XYZ")
    expect(rc.counter).to eq 0

    # delete budget period => delete counter
    bp.delete
    expect(RequestCounter.count).to eq 0

    # r = FactoryBot.create(:request, budget_period_id: bp.id)
    # expect(r.short_id).to eq "XYZ.001"
  end
end
