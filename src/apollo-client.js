// import f from 'lodash'
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
  cache: new InMemoryCache({
    // dataIdFromObject: getIdFromObject
  })
})

// // support `{id: id}` or `{id: { value: id }}`
// function getIdFromObject(object) {
//   // console.log('getIdFromObject', { object })
//
//   const id = idPerType(object)
//   // if (!id) {
//   //   throw new Error('Could not find ID! \n\n' + JSON.stringify(object, 0, 2))
//   // }
//   return id
// }
//
// function idPerType(object) {
//   switch (object.__typename) {
//     case 'Request':
//       return object.id.value
//     case 'RequestFieldID':
//       return object.value
//     default:
//       return object.id
//   }
// }
