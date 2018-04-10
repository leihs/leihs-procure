module Config
  module Database
    def database
      Sequel.postgres(host: 'localhost', database: 'leihs_test')
    end
  end
end
