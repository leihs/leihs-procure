require 'spec_helper'

describe 'short id' do
  it 'works' do
    # create 1st budget period => create 1st counter
    bp1 = FactoryBot.create(:budget_period, :requesting_phase, name: "XYZ")
    rc1 = RequestCounter.find(prefix: "XYZ")
    expect(RequestCounter.count).to eq 1
    expect(rc1.counter).to eq 0

    # delete 1st budget period => delete 1st counter
    bp1.delete
    expect(RequestCounter.count).to eq 0

    # create 1st budget period again => create 1st counter again
    bp1 = FactoryBot.create(:budget_period, :requesting_phase, name: "XYZ")
    rc1 = RequestCounter.find(prefix: "XYZ")
    expect(RequestCounter.count).to eq 1
    expect(rc1.counter).to eq 0

    # create request for the 1st budget period => update 1st counter and get short_id
    r = FactoryBot.create(:request, budget_period_id: bp1.id)
    expect(r.short_id).to eq "XYZ.001"
    rc1 = RequestCounter.find(prefix: "XYZ")
    expect(rc1.counter).to eq 1

    # rename 1st budget period => create 2nd counter
    bp1.update(name: "ABC")
    rc2 = RequestCounter.find(prefix: "ABC")
    expect(RequestCounter.count).to eq 2
    expect(rc2.counter).to eq 0

    # create request for renamed 1st the budget period => update 2nd counter and get short_id
    r = FactoryBot.create(:request, budget_period_id: bp1.id)
    expect(r.short_id).to eq "ABC.001"
    rc2 = RequestCounter.find(prefix: "ABC")
    expect(rc2.counter).to eq 1

    # create 2nd budget period with the old name of the 1st one => don't create any counter
    bp2 = FactoryBot.create(:budget_period, :requesting_phase, name: "XYZ")
    rc1 = RequestCounter.find(prefix: "XYZ")
    expect(RequestCounter.count).to eq 2
    expect(rc1.counter).to eq 1
    
    # create request for the 2nd budget period => update 1st counter and get short_id
    r = FactoryBot.create(:request, budget_period_id: bp2.id)
    expect(r.short_id).to eq "XYZ.002"
    rc1 = RequestCounter.find(prefix: "XYZ")
    expect(rc1.counter).to eq 2

    # check that the 1st counter stayed the same
    expect(rc2.reload.counter).to eq 1
  end
end
