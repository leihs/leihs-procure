import React from 'react'
// import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from '../GraphQlFragments'
import Loading from '../components/Loading'
import RequestsListFiltered from '../components/RequestsListFiltered'

class RequestsIndexWithData extends React.Component {
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
    this.setState({
      currentFilters: { ...this.state.currentFilters, ...filters }
    })
  }
  render({ state } = this) {
    const filters = state.currentFilters
    // FIXME: use real variables, not String interpolation!
    const query = gql`
      query RequestsIndex($budgetPeriods: [ID]) {
        # for filterbar:
        budget_periods {
          id
          name
        }
        # main index:
        requests(budget_period_id: ${JSON.stringify(filters.budgetPeriods)}) {
          ...RequestFieldsForIndex
        }
      }
      ${Fragments.RequestFieldsForIndex}
    `

    const render = ({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <p>Error :(</p>

      return (
        <RequestsListFiltered
          requests={data.requests}
          filters={{ available: { budgetPeriods: data.budget_periods } }}
          onFilterChange={this.onFilterChange}
        />
      )
    }

    return <Query query={query}>{render}</Query>
  }
}

export default RequestsIndexWithData
