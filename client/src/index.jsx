// polyfills first
import 'react-app-polyfill/ie11'

// non-polyfill imports
import React from 'react'
import Notification from './components/Notification'
import ReactDOM from 'react-dom'
import { createRoot } from 'react-dom/client'
import f from 'lodash'
import { Routes, Route, BrowserRouter, Navigate } from 'react-router-dom'
import { ApolloProvider } from '@apollo/client'

import lodashMixins from './lodash-mixins'
import { apolloClient } from './apollo-client'
import App from './containers/App'
// import { Navigate } from './components/Router'
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
import { useNavigate } from 'react-router-dom'

// webpack: inject styles
import './styles/index.css'

// lodash setup
f.mixin(lodashMixins)

// dev helpers
initDevHelpers()

const baseName = import.meta.env.BASE_URL // set in package.json/homepage
function Root() {
  return (
    <>
      <ApolloProvider client={apolloClient}>
        <BrowserRouter basename={baseName} forceRefresh={!supportsHistory}>
          <App isDev={isDev}>
            <Notification />
            <Routes>
              {/* <Route
              path="/"
              render={() => <Navigate to="/procure/requests/" />}
            /> */}
              <Route path="/" element={<HomePage />} />
              <Route path="/requests" element={<RequestsIndex />} />
              <Route path="/requests/new" element={<RequestNew />} />
              <Route path="/requests/:id" element={<RequestShow />} />
              <Route path="/admin/users" element={<AdminUsers />} />
              <Route path="/admin/categories/*" element={<AdminCategories />} />
              <Route path="/admin/organizations" element={<AdminOrgs />} />
              <Route
                path="/admin/budget-periods"
                element={<AdminBudgetPeriods />}
              />
              <Route path="/admin/settings" element={<AdminSettings />} />
              <Route path="/templates/edit" element={<TemplatesEdit />} />
              <Route
                path="/templates*/*"
                render={() => <Navigate to="/templates/edit" />}
              />

              <Route strict path="/dev/playground" element={<DevUiCatalog />} />
              <Route strict path="/dev/console" element={<DevConsole />} />

              <Route
                element={() => <center className="h1">404 not found</center>}
              />
            </Routes>
          </App>
        </BrowserRouter>
      </ApolloProvider>
    </>
  )
}

const container = document.getElementById('app')
const root = createRoot(container) // createRoot(container!) if you use TypeScript
root.render(<Root />)

//
function initDevHelpers() {
  window.f = f
  window.debugObj = obj => {
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
