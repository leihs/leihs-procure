require 'digest'

class ApiToken < Sequel::Model(:api_tokens)
  set_primary_key :id
  attr_accessor :token_secret
end

FactoryBot.define do
  factory :api_token do
    token_secret { Faker::Creature::Cat.name }
    token_part { token_secret[0..4] }
    token_hash {
      database[ "SELECT crypt(?,gen_salt('bf')) AS pw_hash",
        token_secret].first[:pw_hash]
    }
  end
end
