; Used for development purposes. Copy to profiles.clj and adjust accordingly.

{:profiles/dev
   {:env {:leihs-database-url
            "jdbc:postgresql://localhost:5432/leihs_dev?max-pool-size=5",
          :leihs-http-base-url "http://localhost:3333"}},
 :profiles/test
   {:env {:leihs-database-url
            "jdbc:postgresql://localhost:5432/leihs_test?max-pool-size=5",
          :leihs-http-base-url "http://localhost:3333"}}}
