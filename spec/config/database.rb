
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
