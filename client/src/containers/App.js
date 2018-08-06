import React, { Component, Fragment as F } from 'react'
import f from 'lodash'
import { withRouter } from 'react-router'
import { Query } from 'react-apollo'

import Loading from '../components/Loading'
import { ErrorPanel, FatalErrorScreen, getErrorCode } from '../components/Error'
import MainNav from '../components/MainNav'
import { CURRENT_USER_QUERY } from './CurrentUserProvider'

const MainNavWithRouter = withRouter(MainNav)

// NOTE: uses fetchPolicy="cache-then-network" to be quick on refreshes
//       but also make sure the data is correct and connection is possible.
//       We re-use the query from `CurrentUserProvider` (to ensure caching),
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
              return (
                <ErrorHandler error={error} data={data} refetch={refetch} />
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

const ErrorHandler = ({ error, data, refetch }) => {
  const errCode = getErrorCode(error)
  const retryButton = (
    <button
      type="button"
      className="btn btn-sm btn-outline-dark"
      onClick={e => refetch()}
    >
      retry
    </button>
  )

  if (
    errCode === 'NO_CONNECTION_TO_SERVER' ||
    errCode === 'UNKNOWN_NETWORK_ERROR'
  ) {
    return (
      <FatalErrorScreen error={error}>
        <p>You are offline or the Server is down!</p>
        <p>{retryButton}</p>
      </FatalErrorScreen>
    )
  }

  // FIXME: handle 401 when backend is fixed, all-in-1 msg for now:
  if (errCode === 'NOT_AUTHORIZED_FOR_APP') {
    return (
      <FatalErrorScreen error={error}>
        <p>
          You are not allowed to use this application!
          <br />
          Try going to the <a href="/">home page</a> and maybe log in.
        </p>
      </FatalErrorScreen>
    )
  }
  if (f.isString(errCode)) {
    return (
      <FatalErrorScreen error={error}>
        <p>
          Sorry, there was an error of type <samp>{errCode}</samp>.
        </p>
        <p>{retryButton}</p>
      </FatalErrorScreen>
    )
  }
  // only show error panel if no error code is found (really unexpected)
  return (
    <FatalErrorScreen title={false}>
      <div title={false}>
        <ErrorPanel error={error} data={data} />
        {retryButton}
      </div>
    </FatalErrorScreen>
  )
}
