import React, { Component } from 'react'
import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'
import { ErrorPanel } from '../components/Error'

// NOTE: its ok to use this multiple times per view because the data is cached!

export const CURRENT_USER_QUERY = gql`
  query me {
    current_user {
      navbarProps
      user {
        id
        firstname
        lastname
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
    settings {
      contact_url
    }
  }
`

// add some roles shortcutes
export const UserWithShortcuts = user => {
  const Roles = me => {
    const p = me.user.permissions
    const r = f.pick(p, 'isAdmin', 'isRequester')
    r.isInspector = f.some(p.isInspectorForCategories)
    r.isViewer = f.some(p.isViewerForCategories)
    r.isOnlyViewer =
      r.isViewer && !(r.isAdmin || r.isInspector || r.isRequester)
    r.isOnlyRequester =
      r.isRequester && !(r.isAdmin || r.isInspector || r.isViewer)
    return r
  }
  return { ...user, roles: Roles(user) }
}

class CurrentUserProvider extends Component {
  render({ props: { children, ...props } } = this) {
    return (
      <Query query={CURRENT_USER_QUERY} {...props}>
        {({ loading, error, data }) => {
          // NOTE: never show loading spinner bc data is virtually always
          // in cache already. Do show Errors just in case to not be silent.
          if (loading) return false
          if (error) return <ErrorPanel error={error} data={data} />
          const me = UserWithShortcuts(data.current_user)
          return children(me)
        }}
      </Query>
    )
  }
}

export default CurrentUserProvider
