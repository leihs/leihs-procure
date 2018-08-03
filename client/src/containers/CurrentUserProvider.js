import React, { Component } from 'react'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'
import { ErrorPanel } from '../components/Error'

// NOTE: its ok to use this multiple times per view because the data is cached!

export const CURRENT_USER_QUERY = gql`
  query me {
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

class CurrentUserProvider extends Component {
  render({ props: { children } } = this) {
    return (
      <Query query={CURRENT_USER_QUERY}>
        {({ loading, error, data }) => {
          // NOTE: never show loading spinner bc data is virtually always
          // in cache already. Do show Errors just in case to not be silent.
          if (loading) return false
          if (error) return <ErrorPanel error={error} data={data} />
          return children(data.current_user)
        }}
      </Query>
    )
  }
}

export default CurrentUserProvider
