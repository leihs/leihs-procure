import React from 'react'
// import cx from 'classnames'
// import f from 'lodash'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

// import t from '../../locale/translate'
// import { Div } from '../../components/Bootstrap'
import Icon from '../../components/Icons'
import Loading from '../../components/Loading'
import { MainWithSidebar } from '../../components/Layout'
// import { DisplayName } from '../../components/decorators'
import { ErrorPanel } from '../../components/Error'

// # DATA
//
const ADMIN_TEMPLATES_PAGE_QUERY = gql`
  query adminTemplates {
    templates {
      id
      can_delete
      article_name
      article_number
      model {
        id
        product
        version
      }
      price_cents
      price_currency
      supplier {
        id
      }
    }
  }
`

// # PAGE
//
const AdminTemplates = () => (
  <Query query={ADMIN_TEMPLATES_PAGE_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <ErrorPanel error={error} data={data} />

      return (
        <MainWithSidebar>
          <h1 className="mb-4">
            <Icon.Templates /> Vorlagen
          </h1>

          <code>TBD view + edit</code>

          <pre>
            <mark>{JSON.stringify(data, 0, 2)}</mark>
          </pre>
        </MainWithSidebar>
      )
    }}
  </Query>
)

export default AdminTemplates
