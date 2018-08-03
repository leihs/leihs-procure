import React, { Component } from 'react'
import f from 'lodash'
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

// add some roles shortcutes
const Roles = me => {
  const p = me.user.permissions
  const r = f.pick(p, 'isAdmin', 'isRequester')
  r.isInspector = f.some(p.isInspectorForCategories)
  r.isViewer = f.some(p.isViewerForCategories)
  r.isOnlyRequester =
    r.isRequester && !(r.isAdmin || r.isInspector || r.isViewer)
  return r
}

class CurrentUserProvider extends Component {
  render({ props: { children } } = this) {
    return (
      <Query query={CURRENT_USER_QUERY}>
        {({ loading, error, data }) => {
          // NOTE: never show loading spinner bc data is virtually always
          // in cache already. Do show Errors just in case to not be silent.
          if (loading) return false
          if (error) return <ErrorPanel error={error} data={data} />
          const me = { ...data.current_user, roles: Roles(data.current_user) }
          return children(me)
        }}
      </Query>
    )
  }
}

export default CurrentUserProvider
