import ApolloClient from 'apollo-boost'
import { InMemoryCache, defaultDataIdFromObject } from 'apollo-cache-inmemory'
import { isDev, store } from './env'

import logger from 'debug'
const log = logger('app:apollo')

const CSRF_COOKIE_NAME = 'leihs-anti-csrf-token'
export const endpointURL = '/procure/graphql'
export const defaultHeaders = { accept: 'application/json' }

export const buildAuthHeaders = () =>
  isDev
    ? {
        'X-Fake-Token-Authorization': store.getItem('LEIHS_DEV_FAKE_USER_ID'),
        'X-CSRF-Token': getCookieValue(document.cookie, CSRF_COOKIE_NAME)
      }
    : { 'X-CSRF-Token': getCookieValue(document.cookie, CSRF_COOKIE_NAME) }

export const fetchOptions = {
  credentials: isDev ? 'omit' : 'same-origin' // send the cookie(s)
}

const cache = new InMemoryCache({
  dataIdFromObject: object => {
    // NOTE: workaround buggy apollo cache, use `cacheKey` if given,
    //       otherwise fall back to default handling.
    if (object.cacheKey) return `${object.__typename}:^:${object.cacheKey}`
    return defaultDataIdFromObject(object)
  }
})

export const apolloClient = new ApolloClient({
  cache,
  uri: endpointURL,
  // static options for fetch requests:
  fetchOptions,
  // dynamic options for fetch requests:
  request: operation =>
    operation.setContext({
      headers: { ...defaultHeaders, ...buildAuthHeaders() }
    })
})

// util

export const mutationErrorHandler = err => {
  // not much we can do on backend error
  // eslint-disable-next-line no-console
  console.error(err)
  window.confirm('Error! ' + err) && window.location.reload()
}

// helper

const getCookieValue = (cookies, name) =>
  (
    (cookies || '')
      .split(';')
      .map(s => s.trim())
      .filter(s => s && s.indexOf(name) === 0)[0] || ''
  ).replace(`${name}=`, '')

// DEV FAKE AUTH
isDev &&
  (() => {
    const USER_IDS = {
      mfa: '7da6733c-c819-5613-8cad-2a40f51c90da',
      gasser: 'f721d6b7-8275-5ee0-b225-aa7c13781f45'
    }
    const fakeLogin = nameOrId => {
      store.setItem('LEIHS_DEV_FAKE_USER_ID', USER_IDS[nameOrId] || nameOrId)
      window.location.reload()
    }
    window.LEIHS_DEV_USER_IDS = USER_IDS
    window.fakeLogin = fakeLogin

    const fakeUser = store.getItem('LEIHS_DEV_FAKE_USER_ID')
    if (!fakeUser) fakeLogin('mfa')
    log('RUNNING IN DEV MODE', { fakeUser })
  })()
