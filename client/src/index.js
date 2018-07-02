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
import AdminBudgetPeriods from './pages/AdminBudgetPeriodsPage'
import DevUiCatalog from './pages/_dev/UiCatalogPage'
import DevConsole from './pages/_dev/ConsolePage'

// env: polyfills (browser support)
require('es6-promise').polyfill()
require('isomorphic-fetch')

// webpack: inject styles
require('./styles/index.css')

// lodash setup
f.mixin(lodashMixins)

const baseName = process.env.PUBLIC_URL // set in package.json/homepage
const supportsHistory = 'pushState' in window.history
const isDev = process.env.NODE_ENV === 'development'

const Root = () => (
  <ApolloProvider client={apolloClient}>
    <BrowserRouter basename={baseName} forceRefresh={!supportsHistory}>
      <App isDev={isDev}>
        <Switch>
          <Route exact path="/" render={() => <Redirect to="/requests" />} />
          <Route exact path="/requests" component={RequestsIndex} />
          <Route exact path="/admin/users" component={AdminUsers} />
          <Route path="/admin/categories" component={AdminCategories} />
          <Route path="/admin/organizations" component={AdminOrgs} />
          <Route path="/admin/budget-periods" component={AdminBudgetPeriods} />

          <Route strict path="/dev/playground" component={DevUiCatalog} />
          <Route strict path="/dev/console" component={DevConsole} />

          <Route
            component={() => <center className="h1">404 not found</center>}
          />
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
window.debug = () => (localStorage.debug = 'app:*')
window.nodebug = () => (localStorage.debug = '')
