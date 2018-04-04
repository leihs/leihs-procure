import React from 'react'
import ReactDOM from 'react-dom'

import './styles.css'

import App from './App'
import registerServiceWorker from './registerServiceWorker'

import ApolloClient from 'apollo-boost'
import { ApolloProvider } from 'react-apollo'

const client = new ApolloClient({
  uri: '/graphql'
})

const Root = () => (
  <ApolloProvider client={client}>
    <App />
  </ApolloProvider>
)
ReactDOM.render(<Root />, document.getElementById('root'))
registerServiceWorker()
