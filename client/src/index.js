// polyfills first
import 'react-app-polyfill/ie11'

// non-polyfill imports
import React from 'react'
import ReactDOM from 'react-dom'
import f from 'lodash'
import { Switch, Route, BrowserRouter } from 'react-router-dom'
import { ApolloProvider } from 'react-apollo'

import lodashMixins from './lodash-mixins'
import { apolloClient } from './apollo-client'
import App from './containers/App'
import { Redirect } from './components/Router'
import { isDev, supportsHistory } from './env'

// all the pages
import HomePage from './pages/HomePage'
import RequestsIndex from './pages/RequestsIndexPage'
import RequestShow from './pages/RequestShowPage'
import RequestNew from './pages/RequestNewPage'

import AdminUsers from './pages/admin/AdminUsersPage'
import AdminCategories from './pages/admin/AdminCategoriesPage'
import AdminOrgs from './pages/admin/AdminOrgsPage'
import AdminBudgetPeriods from './pages/admin/AdminBudgetPeriodsPage'
import TemplatesEdit from './pages/TemplatesEditPage'
import AdminSettings from './pages/admin/AdminSettingsPage'

import DevUiCatalog from './pages/_dev/UiCatalogPage'
import DevConsole from './pages/_dev/ConsolePage'

// webpack: inject styles
require('./styles/index.css')

// lodash setup
f.mixin(lodashMixins)

// dev helpers
initDevHelpers()

const baseName = process.env.PUBLIC_URL // set in package.json/homepage

const Root = () => (
  <ApolloProvider client={apolloClient}>
    <BrowserRouter basename={baseName} forceRefresh={!supportsHistory}>
      <App isDev={isDev}>
        <Switch>
          <Route exact path="/" component={HomePage} />
          <Route exact path="/requests" component={RequestsIndex} />
          <Route exact path="/requests/new" component={RequestNew} />
          <Route exact path="/requests/:id" component={RequestShow} />
          <Route exact path="/admin/users" component={AdminUsers} />
          <Route path="/admin/categories" component={AdminCategories} />
          <Route path="/admin/organizations" component={AdminOrgs} />
          <Route path="/admin/budget-periods" component={AdminBudgetPeriods} />
          <Route path="/admin/settings" component={AdminSettings} />

          <Route path="/templates/edit" component={TemplatesEdit} />
          <Route
            path="/templates**"
            render={() => <Redirect to="/templates/edit" />}
          />

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

//
function initDevHelpers() {
  window.f = f
  window.debugObj = obj => {
    console.debug(obj) // eslint-disable-line no-console
    return obj
  }

  window.debug = () => {
    localStorage.debug = 'app:*'
  }
  window.nodebug = () => {
    localStorage.debug = ''
  }
  Object.defineProperty(window, 'isDebug', {
    get: () => /\*|(app:)/.test(localStorage.debug)
  })
}
