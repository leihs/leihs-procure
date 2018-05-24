# leihs-procurement

WIP

## for devs

1. clone this repo
2. copy `profiles_template.clj` to `profiles.clj`
3. adjust the `LEIHS_DATABASE_URL` and `LEIHS_HTTP_BASE_URL` in this file according to your local needs
4. `lein run "run"`
5. `graphiql` is now available at `http://LEIHS_HTTP_BASE_URL/procure/graphiql/index.html`

You can mock the authenticated user by setting request's header: `Authorization: user_id` for POST or
`user_id` query param for GET requests.

### for running tests locally

1. run the server by `lein with-profile test run "run"`
2. bundle exec rspec path-to-file

You can also work with environmental variables instead of `profiles.clj` if prefered. If used both, the
environmental variables take precedence.
