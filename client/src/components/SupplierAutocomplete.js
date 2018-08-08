import React from 'react'
import gql from 'graphql-tag'

import { DisplayName } from './decorators'
import InlineSearch from './InlineSearch'

const SEARCH_SUPPLIERS_QUERY = gql`
  query searchSupplier($searchTerm: String! = "") {
    suppliers(search_term: $searchTerm, limit: 25, offset: 0) {
      id
      name
    }
  }
`

const SupplierAutocomplete = ({ onSelect }) => (
  <InlineSearch
    searchQuery={SEARCH_SUPPLIERS_QUERY}
    itemToString={DisplayName}
    onSelect={onSelect}
  />
)

export default SupplierAutocomplete
