import React from 'react'
import gql from 'graphql-tag'

import InlineSearch from './InlineSearch'

const SEARCH_USERS_QUERY = gql`
  query($searchTerm: String!) {
    users(search_term: $searchTerm, limit: 25) {
      id
      firstname
      lastname
    }
  }
`

const UserAutocomplete = ({ onSelect }) => (
  <InlineSearch
    searchQuery={SEARCH_USERS_QUERY}
    itemToString={({ firstname, lastname }) => `${firstname} ${lastname}`}
    onSelect={onSelect}
  />
)

export default UserAutocomplete
