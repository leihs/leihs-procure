# leihs-procurement

WIP

## for devs

1. clone this repo
2. `export LEIHS_DATABASE_URL=...`
   * default is `jdbc:postgresql://leihs:leihs@localhost:5432/leihs?max-pool-size=5`
3. `export LEIHS_HTTP_BASE_URL=...`
   * default is `http://localhost:3211`
4. `lein run "run"`
5. `graphiql` is now available at `http://localhost:3211/procure/graphiql/index.html`

In `src/dev/leihs/procurement/mock.clj` you can change the authenticated `user-id`.

### for running tests locally

1. `./dev/start-test-server.sh`
2. in another terminal window: `bundle exec rspec`
