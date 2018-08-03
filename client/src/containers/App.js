import React, { Component, Fragment as F } from 'react'
import f from 'lodash'
import { withRouter } from 'react-router'
import { Query } from 'react-apollo'

import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import MainNav from '../components/MainNav'
import { CURRENT_USER_QUERY } from './CurrentUserProvider'

const MainNavWithRouter = withRouter(MainNav)

// NOTE: uses fetchPolicy="cache-then-network" to be quick on refreshes
//       but also make sure the data is correct and connection is possible.
//       We re-use the Query from `CurrentUserProvider` (to ensure caching),
//       but not the component because the "AppShell" handles errors differently.

class App extends Component {
  render({ props: { children, isDev } } = this) {
    return (
      // TODO: set lang to instance default language
      <div className="ui-app" lang="de">
        <Query
          query={CURRENT_USER_QUERY}
          fetchPolicy="cache-then-network"
          notifyOnNetworkStatusChange
        >
          {({ error, loading, data, refetch }) => {
            if (loading) return <Loading />

            if (error) {
              const errCode = getErrorCode(error)
              if (errCode === 'NO_CONNECTION_TO_SERVER') {
                return (
                  <FatalErrorScreen>
                    You are offline or the Server is down!<br />
                    {retryButton(refetch)}
                  </FatalErrorScreen>
                )
              }

              // FIXME: handle 401 when backend is fixed, all-in-1 msg for now:
              if (errCode === 'NOT_AUTHORIZED_FOR_APP') {
                return (
                  <FatalErrorScreen>
                    You are not allowed to use this application!<br />
                    Try going to the <a href="/">home page</a> and maybe log in.
                  </FatalErrorScreen>
                )
              }
              return (
                <FatalErrorScreen>
                  <ErrorPanel error={error} data={data} />
                  {retryButton(refetch)}
                </FatalErrorScreen>
              )
            }

            return (
              <F>
                <MainNavWithRouter isDev={isDev} me={data.current_user.user} />
                <div className="minh-100vh">{children}</div>
              </F>
            )
          }}
        </Query>
      </div>
    )
  }
}

export default App

const FatalErrorScreen = ({ children }) => (
  <div className="m-3 p-5 text-center">{children}</div>
)

const retryButton = fn => (
  <button className="btn btn-sm btn-outline-dark" onClick={fn}>
    retry
  </button>
)

const getErrorCode = error => {
  const code = f.first(f.map(f.get(error, 'graphQLErrors'), 'extensions.code'))
  if (code) return code

  if (f.get(error, 'networkError.message') === 'Failed to fetch') {
    return 'NO_CONNECTION_TO_SERVER'
  }

  if (f.get(error, 'networkError')) {
    return 'UNKNOWN_NETWORK_ERROR'
  }
}
