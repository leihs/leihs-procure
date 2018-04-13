import React from 'react'
// import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from '../queries/fragments'
import RequestsListFiltered from '../components/RequestsListFiltered'

// NOTE: there are 2 separate queries for filterbar and request list.
// they are not coordinated in any way (yet), so for now its a 2-step loading UI.
// will revisit when setup is complete – could be a non-problem in the end,
// if, like in v1, the user's filter settings are persisted in DB
// (bc the first query can get everything in 1 fetch)

export const CURRENT_USER_QUERY = gql`
  query CurrentUser {
    CurrentUser {
      user {
        id
        login
        firstname
        lastname
      }
      savedFilters {
        sort_by
        sort_dir
        search
        category_ids
        categories_with_requests
        organization_ids
        priorities
        inspector_priorities
        states
        budget_period_ids
      }
    }
  }
`

const FILTERS_QUERY = gql`
  query RequestFilters {
    budget_periods {
      id
      name
    }
  }
`

const REQUESTS_QUERY = gql`
  query RequestsIndexFiltered($budgetPeriods: [ID]) {
    # main index:
    requests(budget_period_id: $budgetPeriods) {
      ...RequestFieldsForIndex
    }
  }
  ${Fragments.RequestFieldsForIndex}
`

class RequestsIndexPage extends React.Component {
  constructor() {
    super()
    this.state = {
      currentFilters: {
        budgetPeriods: ['2292d02b-44cc-4342-8c76-0cc29ff7a92b']
      }
    }
    this.onFilterChange = this.onFilterChange.bind(this)
  }
  onFilterChange(filters) {
    this.setState(state => ({
      currentFilters: { ...state.filters, ...filters }
    }))
  }
  render({ state } = this) {
    return (
      <Query query={FILTERS_QUERY}>
        {filtersData => {
          return (
            <Query query={REQUESTS_QUERY} variables={state.currentFilters}>
              {requestsData => {
                return (
                  <RequestsListFiltered
                    requests={requestsData}
                    filters={filtersData}
                    currentFilters={state.currentFilters}
                    onFilterChange={this.onFilterChange}
                  />
                )
              }}
            </Query>
          )
        }}
      </Query>
    )
  }
}

export default RequestsIndexPage
