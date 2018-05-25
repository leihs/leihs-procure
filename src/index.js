import React from 'react'
import ReactDOM from 'react-dom'
import f from 'lodash'
import { Switch, Route } from 'react-router-dom'
import { BrowserRouter } from 'react-router-dom'
import { ApolloProvider } from 'react-apollo'

import { apolloClient } from './apollo-client'

// webpack: inject styles
import './styles/index.css'

import App from './components/App'

// all the pages
import RequestsIndex from './pages/RequestsIndexPage'
import AdminUsers from './pages/AdminUsersPage'
import AdminCategories from './pages/AdminCategoriesPage'

const Root = () => (
  <ApolloProvider client={apolloClient}>
    <BrowserRouter>
      <App>
        <Switch>
          <Route exact path="/" component={RequestsIndex} />
          <Route exact path="/admin/users" component={AdminUsers} />
          <Route exact path="/admin/categories" component={AdminCategories} />
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
