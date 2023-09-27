import React from 'react'
// import f from 'lodash'
import gql from 'graphql-tag'
import { useQuery } from '@apollo/client'
import Loading from './Loading'
import { ErrorPanel } from './Error'

// NOTE: just a simple Select/Dropdown for now
// import { DisplayName } from './decorators'
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

function BuildingAutocomplete(props) {
  const { loading, error, data } = useQuery(GET_BUILDINGS_QUERY)

  if (loading) return <Loading />
  if (error) return <ErrorPanel error={error} data={data} />

  return <Select options={optionsFromList(data.buildings)} {...props} />
}

export default BuildingAutocomplete
