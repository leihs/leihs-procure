import React from 'react'
import gql from 'graphql-tag'

import { DisplayName } from './decorators'
import InlineSearch from './InlineSearch'

const SEARCH_USERS_QUERY = gql`
  query searchUsers(
    $searchTerm: String! = ""
    $excludeIds: [ID]
    $isRequester: Boolean
  ) {
    users(
      search_term: $searchTerm
      limit: 25
      offset: 0
      exclude_ids: $excludeIds
      isRequester: $isRequester
    ) {
      id
      firstname
      lastname
    }
  }
`

const UserAutocomplete = ({
  onSelect,
  excludeIds,
  onlyRequesters,
  ...props
}) => (
  <InlineSearch
    searchQuery={SEARCH_USERS_QUERY}
    queryVariables={{ excludeIds, isRequester: onlyRequesters }}
    itemToString={DisplayName}
    onSelect={onSelect}
    {...props}
  />
)

export default UserAutocomplete
