require 'sequel'
require 'addressable'

DB_ENV = ENV['LEIHS_DATABASE_URL'].presence

def http_uri
  @http_uri ||= \
    Addressable::URI.parse DB_ENV.gsub(/^jdbc:postgresql/,'http').gsub(/^postgres/,'http')
end

def database
  @database ||= \
    Sequel.connect(
      if DB_ENV
        'postgres://' \
          + (http_uri.user.presence || ENV['PGUSER'].presence || 'postgres') \
          + ((pw = (http_uri.password.presence || ENV['PGPASSWORD'].presence)) ? ":#{pw}" : "") \
          + '@' + (http_uri.host.presence || ENV['PGHOST'].presence || ENV['PGHOSTADDR'].presence || 'localhost') \
          + ':' + (http_uri.port.presence || ENV['PGPORT'].presence || 5432).to_s \
          + '/' + ( http_uri.path.presence.try(:gsub,/^\//,'') || ENV['PGDATABASE'].presence || 'leihs') \
          + '?pool=5'
      else
        'postgresql://leihs:leihs@localhost:5432/leihs?pool=5'
      end
    )
end

RSpec.configure do |config|
  config.before :each  do
    clean_db
    system("DATABASE_NAME=#{http_uri.basename} ./server/database/scripts/restore-seeds")
  end
end

private

def clean_db
  database[ <<-SQL.strip_heredoc
    SELECT table_name
      FROM information_schema.tables
    WHERE table_type = 'BASE TABLE'
    AND table_schema = 'public'
    AND table_name NOT IN ('schema_migrations','ar_internal_metadata')
    ORDER BY table_type, table_name;
            SQL
  ].map{|r| r[:table_name]}.join(', ').tap do |tables|
    database.run" TRUNCATE TABLE #{tables} CASCADE; "
  end
end


def set_settings
  fail unless LEIHS_PROCURE_HTTP_BASE_URL.present?
  Setting.first || Setting.create # ensure existance!
  database.run <<-SQL
    UPDATE settings
    SET external_base_url = '#{LEIHS_PROCURE_HTTP_BASE_URL}',
        smtp_default_from_address = 'noreply@example.com'
  SQL
end

def resurrect_general_building
  database.run <<-SQL
    INSERT INTO buildings (id, name)
    VALUES ('#{Leihs::Constants::GENERAL_BUILDING_UUID}', 'general building')
  SQL
end

def resurrect_general_room_for_general_building
  database.run <<-SQL
    INSERT INTO rooms (name, building_id, general)
    VALUES ('general room', '#{Leihs::Constants::GENERAL_BUILDING_UUID}', TRUE)
  SQL
end
