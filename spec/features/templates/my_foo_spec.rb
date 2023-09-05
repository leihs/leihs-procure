require 'spec_helper'
require 'pry'

feature 'Foo' do
  context 'a system-admin exists' do

    before(:each) do 
      @user = FactoryBot.create :user
    end


    context 'Bar' do

      scenario 'Whatever' do

        expect(@user).to be

      end

    end
  end
end
