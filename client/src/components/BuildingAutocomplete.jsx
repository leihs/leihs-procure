import React from 'react'
// import f from 'lodash'
import gql from 'graphql-tag'
import { Query } from 'react-apollo'

// NOTE: just a simple Select/Dropdown for now

// import { DisplayName } from './decorators'
import { ErrorPanel } from './Error'
// import InlineSearch from './InlineSearch'
import { Select, optionsFromList } from './Bootstrap'

const GET_BUILDINGS_QUERY = gql`
  query getBuildings {
    buildings {
      id
      name
    }
  }
`

const BuildingAutocomplete = props => (
  <Query query={GET_BUILDINGS_QUERY}>
    {({ loading, error, data, data: { buildings } }) => {
      if (loading) return 'Loading'
      if (error) {
        return <ErrorPanel error={error} data={data} />
      }
      return <Select options={optionsFromList(buildings)} {...props} />
    }}
  </Query>
)

export default BuildingAutocomplete
