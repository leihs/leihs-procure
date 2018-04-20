import gql from 'graphql-tag'

export const FILTERS_QUERY = gql`
  query RequestFilters {
    budget_periods {
      id
      name
    }
    categories {
      id
      name
    }
    organizations {
      id
      name
      shortname
    }
    # priorities {
    #   index
    #   name
    # }
  }
`
