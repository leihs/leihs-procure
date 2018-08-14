# leihs-procurement

WIP

## For devs

1. clone this repo
2. copy `profiles_template.clj` to `profiles.clj`
3. adjust the `LEIHS_DATABASE_URL` and `LEIHS_HTTP_BASE_URL` in this file according to your local needs
4. copy `resources/dev/log4j.properties.template` to `log4j.properties` and adjust according to your local needs
5. `lein run "run"`
6. `graphiql` is now available at `http://LEIHS_HTTP_BASE_URL/procure/graphiql/index.html`

You can mock the authenticated user by setting request's header: `Authorization: user_id` for POST or
`user_id` query param for GET requests.

### Let changes apply on next http request

1. There is a ring middleware which is active with `dev` and `test` profiles. It reloads the code in the changed files. However, this applies only to definitions outside of the main ring handler function. 
2. If you change something on a middleware, then normally rerun of `(-main "run")` is necessary, which among others also recreates the main ring handler function along with the implied middlewares. Precondition is that you have `lein repl` running.
3. Depending on the whole project setup, sometimes a restart of the lein process (`repl` or `run`) is necessary.
4. Sometimes even `lein clean`.
5. And before starting to fill in the resignment form, one can hope that `rm ~/.m2/repository/*` will do.

### Running tests locally

1. run the server by `lein with-profile test run "run"`
2. bundle exec rspec path-to-file

You can also work with environmental variables instead of `profiles.clj` if prefered. If used both, the
environmental variables take precedence.
