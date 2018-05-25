import React from 'react'
import gql from 'graphql-tag'

import { DisplayName } from './decorators'
import InlineSearch from './InlineSearch'

const SEARCH_USERS_QUERY = gql`
  query searchUsers($searchTerm: String! = "", $excludeIds: [ID]) {
    users(
      search_term: $searchTerm
      limit: 25
      offset: 0
      exclude_ids: $excludeIds
    ) {
      id
      firstname
      lastname
    }
  }
`

const UserAutocomplete = ({ onSelect, excludeIds }) => (
  <InlineSearch
    searchQuery={SEARCH_USERS_QUERY}
    queryVariables={{ excludeIds }}
    itemToString={DisplayName}
    onSelect={onSelect}
  />
)

export default UserAutocomplete
