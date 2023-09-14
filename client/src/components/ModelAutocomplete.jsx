import React from 'react'
import gql from 'graphql-tag'

import { DisplayName } from './decorators'
import InlineSearch from './InlineSearch'

const SEARCH_MODELS_QUERY = gql`
  query searchModel($searchTerm: String! = "") {
    models(search_term: $searchTerm, limit: 25, offset: 0) {
      id
      product
      version
      # FIXME: schema has name but does not work on request.model.value!
      # name
    }
  }
`

const ModelAutocomplete = props => (
  <InlineSearch
    searchQuery={SEARCH_MODELS_QUERY}
    itemToString={DisplayName}
    {...props}
  />
)

export default ModelAutocomplete
