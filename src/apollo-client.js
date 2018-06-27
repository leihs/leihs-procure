// import f from 'lodash'
import { ApolloClient } from 'apollo-boost'
import { InMemoryCache } from 'apollo-boost'
import { HttpLink } from 'apollo-boost'

// // TMP DEV FAKE AUTH
// const USER_IDS = {
//   mfa: '7da6733c-c819-5613-8cad-2a40f51c90da',
//   gasser: 'f721d6b7-8275-5ee0-b225-aa7c13781f45'
// }
//
// window.CURRENT_USER_ID = USER_IDS.gasser

// for dynamic header see <https://www.apollographql.com/docs/react/recipes/authentication.html#Header>
export const apolloClient = new ApolloClient({
  link: new HttpLink({
    uri: '/procure/graphql',
    // headers: {
    //   authorization: window.CURRENT_USER_ID
    // }
    credentials: 'same-origin'
  }),
  cache: new InMemoryCache()
})
