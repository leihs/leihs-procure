import React, { Component, Fragment as F } from 'react'
import f from 'lodash'
import { withRouter } from 'react-router'
import { Query } from 'react-apollo'

import Loading from '../components/Loading'
import { ErrorPanel, FatalErrorScreen, getErrorCode } from '../components/Error'
import {
  AlertDismissable,
  RouteParams as Routed
} from '../components/Bootstrap'
import MainNav from '../components/MainNav'
import { CURRENT_USER_QUERY } from './CurrentUserProvider'

const MainNavWithRouter = withRouter(MainNav)

// NOTE: uses fetchPolicy="cache-and-network" to be quick on refreshes
//       but also make sure the data is correct and connection is possible.
//       We re-use the query from `CurrentUserProvider` (to ensure caching),
//       but not the component because the "AppShell" handles errors differently.
//
// FIXME: when using `refetch` to reload, subsequent requests
//      are merged into the previous ones, breaking error handling!

class App extends Component {
  render({ props: { children, isDev } } = this) {
    return (
      <Routed>
        {({ location }) => {
          const locationKey = location.key || JSON.stringify(location)

          return (
            // TODO: set lang to instance default language
            <div className="ui-app" lang="de">
              <Query
                key={locationKey}
                query={CURRENT_USER_QUERY}
                fetchPolicy="cache-and-network"
                notifyOnNetworkStatusChange
              >
                {({ error, loading, data, refetch, networkStatus }) => {
                  // refetch *in background* for every navigation,
                  // don't flicker UI by only using `loading`!
                  const isLoading = !(data && data.current_user) && loading

                  if (isLoading) return <Loading />

                  if (error) {
                    return (
                      <ErrorHandler
                        error={error}
                        data={data}
                        refetch={e => window.location.reload()}
                      />
                    )
                  }

                  return (
                    <F>
                      <MainNavWithRouter
                        isDev={isDev}
                        me={data.current_user.user}
                        contactUrl={data.settings.contact_url}
                      />
                      <div className="minh-100vh">
                        <FlashAlert {...f.get(location, 'state.flash')} />
                        {children}
                      </div>
                    </F>
                  )
                }}
              </Query>
            </div>
          )
        }}
      </Routed>
    )
  }
}

export default App

const FlashAlert = ({ message, level = 'info' }) =>
  !!message && (
    <AlertDismissable fade={false} color={level} className="rounded-0">
      {message}
    </AlertDismissable>
  )

const ErrorHandler = ({ error, data, refetch }) => {
  const errCode = getErrorCode(error)
  const retryButton = (
    <button
      type="button"
      className="btn btn-sm btn-outline-dark"
      onClick={refetch}
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
      <p>
        <ErrorPanel error={error} data={data} />
      </p>
      <p>{retryButton}</p>
    </FatalErrorScreen>
  )
}
