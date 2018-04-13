import React from 'react'
import ReactDOM from 'react-dom'
import f from 'lodash'
import ApolloClient from 'apollo-boost'
import { ApolloProvider } from 'react-apollo'

// webpack: inject styles
import './styles/index.css'

// import registerServiceWorker from './registerServiceWorker'
import App from './components/App'

// no router, just 1 page:
import RequestsIndex from './pages/RequestsIndex'

const client = new ApolloClient({
  uri: '/procure/graphql'
})

const Root = () => (
  <ApolloProvider client={client}>
    {/* <React.StrictMode> */}
    <App>
      <RequestsIndex />
    </App>
    {/* </React.StrictMode> */}
  </ApolloProvider>
)
ReactDOM.render(<Root />, document.getElementById('root'))

// registerServiceWorker() // not yet…

// dev helpers
window.f = f
window.debugObj = obj => {
  console.debug(obj) // eslint-disable-line no-console
  return obj
}
