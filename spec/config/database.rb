require 'sequel'

def database
  Sequel.postgres(host: 'localhost', database: 'leihs_test')
end
