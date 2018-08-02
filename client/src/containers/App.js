import React, { Component, Fragment as F } from 'react'
import { withRouter } from 'react-router'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'

import MainNav from '../components/MainNav'

const MainNavWithRouter = withRouter(MainNav)

const APP_SHELL_QUERY = gql`
  query appShell {
    current_user {
      user {
        id
        firstname
        lastname
        login
        email
        permissions {
          isAdmin
          isRequester
          isInspectorForCategories {
            id
          }
          isViewerForCategories {
            id
          }
        }
      }
    }
  }
`

class App extends Component {
  render({ props: { children, isDev } } = this) {
    return (
      // TODO: set lang to instance default language
      <div className="ui-app" lang="de">
        <Query fetchPolicy="cache-then-network" query={APP_SHELL_QUERY}>
          {({ error, loading, data }) => {
            if (loading) return <Loading />
            if (error) return <ErrorPanel error={error} data={data} />
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
