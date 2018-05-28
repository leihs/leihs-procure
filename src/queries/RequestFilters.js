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
    # FIXME: should be 'root_only: false' when UI ready
    organizations(root_only: true) {
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
