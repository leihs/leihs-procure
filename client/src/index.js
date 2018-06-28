import React from 'react'
import ReactDOM from 'react-dom'
import f from 'lodash'
import { Switch, Route, Redirect, BrowserRouter } from 'react-router-dom'
import { ApolloProvider } from 'react-apollo'

import lodashMixins from './lodash-mixins'
import { apolloClient } from './apollo-client'
import App from './components/App'

// all the pages
import RequestsIndex from './pages/RequestsIndexPage'
import AdminUsers from './pages/AdminUsersPage'
import AdminCategories from './pages/AdminCategoriesPage'
import AdminOrgs from './pages/AdminOrgsPage'
import UiPlayground from './pages/_UiPlayground'

// env: polyfills (browser support)
require('es6-promise').polyfill()
require('isomorphic-fetch')

// webpack: inject styles
require('./styles/index.css')

// lodash setup
f.mixin(lodashMixins)

const baseName = process.env.PUBLIC_URL // set in package.json/homepage
const supportsHistory = 'pushState' in window.history
const withPlayground = process.env.NODE_ENV === 'development'

const Root = () => (
  <ApolloProvider client={apolloClient}>
    <BrowserRouter basename={baseName} forceRefresh={!supportsHistory}>
      <App withPlayground={withPlayground}>
        <Switch>
          <Route exact path="/" render={() => <Redirect to="/requests" />} />
          <Route exact path="/requests" component={RequestsIndex} />
          <Route exact path="/admin/users" component={AdminUsers} />
          <Route path="/admin/categories" component={AdminCategories} />
          <Route path="/admin/organizations" component={AdminOrgs} />
          {!!withPlayground && (
            <Route strict path="/playground" component={UiPlayground} />
          )}
          <Route component={() => '404'} />
        </Switch>
      </App>
    </BrowserRouter>
  </ApolloProvider>
)
ReactDOM.render(<Root />, document.getElementById('root'))

// dev helpers
window.f = f
window.debugObj = obj => {
  console.debug(obj) // eslint-disable-line no-console
  return obj
}
