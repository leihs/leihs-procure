import { ApolloClient } from 'apollo-boost'
import { InMemoryCache } from 'apollo-boost'
import { HttpLink } from 'apollo-boost'

// TMP DEV FAKE AUTH
const USER_IDS = { gasser: 'f721d6b7-8275-5ee0-b225-aa7c13781f45' }

// for dynamic header see <https://www.apollographql.com/docs/react/recipes/authentication.html#Header>
export const apolloClient = new ApolloClient({
  link: new HttpLink({
    uri: '/procure/graphql',
    headers: {
      authorization: USER_IDS.gasser
    }
  }),
  cache: new InMemoryCache()
})
