import React from 'react'
// import cx from 'classnames'
import f from 'lodash'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

// import t from '../locale/translate'
// import * as fragments from '../queries/fragments'
// import Icon from '../components/Icons'
// import { Div } from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import { DisplayName } from '../components/decorators'
import { ErrorPanel } from '../components/Error'

// # DATA
//
const ADMIN_ORGS_PAGE_QUERY = gql`
  query AdminOrgs {
    organizations(root_only: true) {
      ...OrgProps
      organizations {
        ...OrgProps
      }
    }
    requesters_organizations {
      id
      user {
        id
        firstname
        lastname
      }
      organization {
        id
        name
      }
    }
  }

  fragment OrgProps on Organization {
    id
    name
    shortname
  }
`

// # PAGE
//
const AdminOrgsPage = () => (
  <Query query={ADMIN_ORGS_PAGE_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error) return <ErrorPanel error={error} data={data} />

      const departmentsOrgs = f.sortBy(data.organizations, 'name')
      const requestersByOrg = f.groupBy(
        data.requesters_organizations,
        'organization.id'
      )

      return (
        <MainWithSidebar>
          <h1>Organisations of the requesters</h1>
          <ul className="list-unstyled mt-2">
            {departmentsOrgs.map(dep => (
              <li key={dep.id}>
                <h2 className="h3">{dep.shortname || dep.name}</h2>
                {!f.isEmpty(dep.organizations) && (
                  <ul className="list-unstyled mb-4">
                    {dep.organizations.map(org => (
                      <li key={org.id}>
                        <h3 className="h5 text-bold mb-0">
                          {org.shortname || org.name}
                        </h3>
                        <ul className="list-unstyled mb-2">
                          {f.map(requestersByOrg[org.id], req => (
                            <li key={req.id}>{DisplayName(req.user)}</li>
                          ))}
                        </ul>
                        <ul />
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </MainWithSidebar>
      )
    }}
  </Query>
)

export default AdminOrgsPage
