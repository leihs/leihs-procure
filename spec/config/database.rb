require 'sequel'

def database
  Sequel.postgres(host: 'localhost', database: 'leihs_test')
end

def clean_db
  sql = <<-SQL
    SELECT table_name
      FROM information_schema.tables
    WHERE table_type = 'BASE TABLE'
    AND table_schema = 'public'
    ORDER BY table_type, table_name;
  SQL

  database[sql]
    .map { |r| r[:table_name] }
    .reject { |tn| tn == 'schema_migrations' }
    .join(', ')
    .tap { |tables| database.run " TRUNCATE TABLE #{tables} CASCADE; " }
end

RSpec.configure do |config|
  config.before :each  do
    clean_db
  end
  config.after :suite do
    clean_db
  end
end
