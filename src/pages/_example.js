import React from 'react'
// import f from 'lodash'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

// import ControlledForm from '../components/ControlledForm'
// import Icon from '../components/Icons'
import { Div } from '../components/Bootstrap'

// # DATA
//
const SOME_PAGE_QUERY = gql`
  query SomePage {
    title
  }
`

// # PAGE
//
const SomePage = () => (
  <Query query={SOME_PAGE_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error)
        return (
          <p>
            Error :( <code>{error.toString()}</code>
          </p>
        )
      return <SomeSection data={data} />
    }}
  </Query>
)

export default SomePage

// # VIEW PARTIALS
//

const SomeSection = props => (
  <Div>
    <h2>Hello World</h2>
    <pre>{JSON.stringify(props, 0, 2)}</pre>
  </Div>
)
