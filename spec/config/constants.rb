require 'edn'
require 'pry'

module Constants
  profiles_data = File.exist?("profiles.clj") && File.open("profiles.clj") do |f|
    EDN
      .read(f)
      .try(:fetch, :"profiles/test")
      .try(:fetch, :env)
  end

  LEIHS_DATABASE_URL = \
    ENV['LEIHS_DATABASE_URL'].presence ||
    profiles_data.try(:fetch, :"leihs-database-url").presence

  LEIHS_HTTP_BASE_URL = \
    ENV['LEIHS_HTTP_BASE_URL'].presence ||
    profiles_data.try(:fetch, :"leihs-http-base-url").presence

  raise 'LEIHS_DATABASE_URL not set!' unless LEIHS_DATABASE_URL
  raise 'LEIHS_HTTP_BASE_URL not set!' unless LEIHS_HTTP_BASE_URL
end
