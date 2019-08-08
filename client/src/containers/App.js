import React, { Component, Fragment as F } from 'react'
import f from 'lodash'
import { withRouter } from 'react-router'
import { Query } from 'react-apollo'

import t from '../locale/translate'
import Loading from '../components/Loading'
import { ErrorPanel, FatalErrorScreen, getErrorCode } from '../components/Error'
import { Alert } from '../components/Bootstrap'
import { Routed } from '../components/Router'
import MainNav from '../components/MainNav'
import { CURRENT_USER_QUERY, UserWithShortcuts } from './CurrentUserProvider'

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
        {({ location, dismissFlash }) => {
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
                  const currentUser = f.get(data, 'current_user')
                  // refetch *in background* for every navigation,
                  // don't flicker UI by only using `loading`!
                  const isLoading = !currentUser && loading

                  if (isLoading) return <Loading />

                  if (error) {
                    return (
                      <F>
                        <div className="minh-100vh">
                          <MainNavWithRouter isDev={isDev} me={false} />
                          <ErrorHandler
                            error={error}
                            data={data}
                            refetch={e => window.location.reload()}
                          />
                        </div>
                      </F>
                    )
                  }

                  // XXX hacky getting locale from navbarprops - should be field on user
                  const userLocale = getUserLocale(data)
                  // XXX global variable for lang - should use props or context
                  if (window && userLocale) window.setLang(userLocale)

                  return (
                    <F>
                      <div lang={userLocale}>
                        <MainNavWithRouter
                          isDev={isDev}
                          me={UserWithShortcuts(currentUser)}
                          contactUrl={f.get(data, 'settings.contact_url')}
                        />
                        <div className="minh-100vh">
                          <FlashAlert
                            flash={f.get(location, 'state.flash')}
                            dismiss={dismissFlash}
                          />
                          {children}
                        </div>
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

const FlashAlert = ({ flash: { message, level = 'info' } = {}, dismiss }) =>
  !!message && (
    <Alert fade={false} color={level} className="rounded-0" toggle={dismiss}>
      {message}
    </Alert>
  )

const ErrorHandler = ({ error, data, refetch }) => {
  const errCode = getErrorCode(error)
  const retryButton = (
    <button
      type="button"
      className="btn btn-sm btn-outline-dark"
      onClick={refetch}
    >
      {t('errors.error_btn_retry')}
    </button>
  )

  if (
    errCode === 'NO_CONNECTION_TO_SERVER' ||
    errCode === 'UNKNOWN_NETWORK_ERROR'
  ) {
    return (
      <FatalErrorScreen error={error}>
        <p>{t('errors.msg_offline')}</p>
        <p>{retryButton}</p>
      </FatalErrorScreen>
    )
  }

  // FIXME: handle 401 when backend is fixed, all-in-1 msg for now:
  if (errCode === 'NOT_AUTHORIZED_FOR_APP') {
    return (
      <FatalErrorScreen error={error}>
        <p>
          {t('errors.msg_unauthorized_1')}
          <br />
          {t('errors.msg_unauthorized_2a')}
          <a href="/">{t('errors.msg_unauthorized_2b')}</a>
          {t('errors.msg_unauthorized_2c')}
        </p>
      </FatalErrorScreen>
    )
  }

  if (f.isString(errCode)) {
    return (
      <FatalErrorScreen error={error}>
        <p>
          {t('errors.msg_error_ofcode_1')}
          <samp>{errCode}</samp>
          {t('errors.msg_error_ofcode_2')}
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

function getUserLocale(data) {
  return f.try(
    () =>
      JSON.parse(data.current_user.navbarProps).config.locales.filter(
        l => l.isSelected
      )[0].locale_name
  )
}
