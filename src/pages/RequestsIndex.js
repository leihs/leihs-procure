import React from 'react'
// import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from '../GraphQlFragments'
import RequestsListFiltered from '../components/RequestsListFiltered'

// NOTE: there are 2 separate queries for filterbar and request list.
// they are not coordinated in any way (yet), so for now its a 2-step loading UI.
// will revisit when setup is complete – could be a non-problem in the end,
// if, like in v1, the user's filter settings are persisted in DB
// (bc the first query can get everything in 1 fetch)

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
    const filtersQuery = gql`
      query RequestFilters {
        budget_periods {
          id
          name
        }
      }
    `

    const requestsQuery = gql`
      query RequestsIndexFiltered($budgetPeriods: [ID]) {
        # main index:
        requests(budget_period_id: $budgetPeriods) {
          ...RequestFieldsForIndex
        }
      }
      ${Fragments.RequestFieldsForIndex}
    `

    return (
      <Query query={filtersQuery}>
        {filtersData => {
          return (
            <Query query={requestsQuery} variables={state.currentFilters}>
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
