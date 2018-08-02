import ApolloClient from 'apollo-boost'
import { InMemoryCache, defaultDataIdFromObject } from 'apollo-cache-inmemory'

import logger from 'debug'
const log = logger('app:apollo')

const CSRF_COOKIE_NAME = 'leihs-anti-csrf-token'

const isDev = process.env.NODE_ENV !== 'production'

// DEV FAKE AUTH
isDev &&
  (() => {
    const USER_IDS = {
      mfa: '7da6733c-c819-5613-8cad-2a40f51c90da',
      gasser: 'f721d6b7-8275-5ee0-b225-aa7c13781f45'
    }

    window.LEIHS_DEV_CURRENT_USER_ID = USER_IDS.gasser

    log('RUNNING IN DEV MODE', { fakeUser: window.LEIHS_DEV_CURRENT_USER_ID })
  })()

export const endpointURL = '/procure/graphql'

export const buildAuthHeaders = () =>
  isDev
    ? { 'X-Fake-Token-Authorization': window.LEIHS_DEV_CURRENT_USER_ID }
    : { 'X-CSRF-Token': getCookieValue(document.cookie, CSRF_COOKIE_NAME) }

export const fetchOptions = {
  credentials: isDev ? 'omit' : 'same-origin' // send the cookie(s)
}

const cache = new InMemoryCache({
  dataIdFromObject: object => {
    // FIXME: workaround buggy apollo cache, dont cache certain types at all!
    switch (object.__typename) {
      case 'MainCategory':
        return Math.random()
      case 'Category':
        return Math.random()
      default:
        return defaultDataIdFromObject(object) // fall back to default handling
    }
  }
})

export const apolloClient = new ApolloClient({
  cache,
  uri: endpointURL,
  // static options for fetch requests:
  fetchOptions,
  // dynamic options for fetch requests:
  request: operation => operation.setContext({ headers: buildAuthHeaders() })
})

// helper

const getCookieValue = (cookies, name) =>
  (
    (cookies || '')
      .split(';')
      .map(s => s.trim())
      .filter(s => s && s.indexOf(name) === 0)[0] || ''
  ).replace(`${name}=`, '')
