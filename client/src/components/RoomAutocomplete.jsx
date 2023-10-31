import React from 'react'
// import f from 'lodash'
import gql from 'graphql-tag'
import { Query } from '@apollo/client/react/components'

// NOTE: just a simple Select/Dropdown for now
// TODO: loading state should look like an empty select (minimize flickering!)

import { DisplayName } from './decorators'
import { ErrorPanel } from './Error'
// import InlineSearch from './InlineSearch'
import { Select, optionsFromList } from './Bootstrap'

const GET_ROOMS_QUERY = gql`
  query getRoomsByBuilding($buildingId: ID!) {
    rooms(building_id: $buildingId) {
      id
      name
      description
    }
  }
`

const RoomAutocomplete = ({ buildingId, ...props }) => {
  const skipped = !buildingId

  return (
    <Query query={GET_ROOMS_QUERY} variables={{ buildingId }} skip={skipped}>
      {({ loading, error, data }) => {
        if (skipped)
          return Select.Placeholder('Select a Building first!', props)
        if (loading) return Select.Placeholder('Loading…', props)
        if (error) {
          return <ErrorPanel error={error} data={data} />
        }
        const rooms = data.rooms.map((r) => ({
          ...r,
          name: DisplayName(r)
        }))

        return <Select options={optionsFromList(rooms)} {...props} />
      }}
    </Query>
  )
}

export default RoomAutocomplete
