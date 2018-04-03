import React from 'react'
import ReactDOM from 'react-dom'

import './styles.css'

import App from './App'
import registerServiceWorker from './registerServiceWorker'

import ApolloClient from 'apollo-boost'
import gql from 'graphql-tag'

const client = new ApolloClient({
  uri: '/graphql'
})

client
  .query({
    query: gql`
      {
        requests {
          id
        }
      }
    `
  })
  .then(data => console.log({ data })) // eslint-disable-line no-console

ReactDOM.render(<App />, document.getElementById('root'))
registerServiceWorker()
