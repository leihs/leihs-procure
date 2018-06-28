import ApolloClient from 'apollo-boost'
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

const buildAuthHeaders = () =>
  isDev
    ? { 'X-Fake-Token-Authorization': window.LEIHS_DEV_CURRENT_USER_ID }
    : { 'X-CSRF-Token': getCSRFToken(document.cookie, CSRF_COOKIE_NAME) }

export const apolloClient = new ApolloClient({
  uri: '/procure/graphql',
  // static options for fetch requests:
  credentials: isDev ? 'omit' : 'same-origin', // send the cookie(s)
  fetchOptions: {
    credentials: isDev ? 'omit' : 'same-origin' // send the cookie(s)
  },
  // dynamic options for fetch requests:
  request: operation => operation.setContext({ headers: buildAuthHeaders() })
})

const getCSRFToken = (cookies, name) =>
  (cookies || '')
    .split(';')
    .map(s => s.trim())
    .filter(s => s && s.indexOf(name) === 0)[0]
    .replace(`${name}=`, '')
