import React from 'react'
import ReactDOM from 'react-dom'

import './styles/index.css'

import App from './components/App'
import registerServiceWorker from './registerServiceWorker'

import ApolloClient from 'apollo-boost'
import { ApolloProvider } from 'react-apollo'

// no router, just 1 page:
import RequestsIndex from './pages/RequestsIndex'

const client = new ApolloClient({
  uri: '/procure/graphql'
})

const Root = () => (
  <ApolloProvider client={client}>
    <App>
      <RequestsIndex />
    </App>
  </ApolloProvider>
)
ReactDOM.render(<Root />, document.getElementById('root'))

// registerServiceWorker() // not yet…
