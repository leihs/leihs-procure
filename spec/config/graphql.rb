module Config
  module Graphql
    def graphql_client
      @c ||= Graphlient::Client.new('http://localhost:3211/procure/graphql')
    end
  end
end
