import React from 'react'
import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from '../queries/fragments'
import { FILTERS_QUERY } from '../queries/RequestFilters'
// import Loading from '../components/Loading'
// import { ErrorPanel } from '../components/Error'
import RequestsListFiltered from '../components/RequestsListFiltered'

// NOTE: there are 2 separate queries for filterbar and request list.
// they are not coordinated in any way (yet), so for now its a 2-step loading UI.
// will revisit when setup is complete – could be a non-problem in the end,
// if, like in v1, the user's filter settings are persisted in DB
// (bc the first query can get everything in 1 fetch)

// const CURRENT_USER_QUERY = gql`
//   query CurrentUser {
//     CurrentUser {
//       user {
//         id
//         login
//         firstname
//         lastname
//       }
//       savedFilters {
//         sort_by
//         sort_dir
//         search
//         category_ids
//         categories_with_requests
//         organization_ids
//         priorities
//         inspector_priorities
//         states
//         budget_period_ids
//       }
//     }
//   }
// `

const REQUESTS_QUERY = gql`
  # NOTE: requests only shown grouped by period > main cat > sub cat > request.
  # Query using distinct entry points bc also empty "groups" are shown,
  # also it makes iterating over them much simpler.
  query RequestsIndexFiltered(
    $budgetPeriods: [ID]
    $categories: [ID]
    $organizations: [ID]
  ) {
    # TODO: filter arg (id: $budgetPeriods)
    budget_periods {
      id
      name
      inspection_start_date
      end_date
    }
    # TODO: filter arg (id: $mainCategories)
    main_categories {
      id
      name
      image_url

      categories {
        id
        name
        # FIXME: remove this when MainCategory.categories scope is fixed
        main_category_id
      }
    }
    requests(
      budget_period_id: $budgetPeriods
      category_id: $categories # organization_id: $organizations
    ) {
      ...RequestFieldsForIndex
    }
  }
  ${Fragments.RequestFieldsForIndex}
`

const REQUEST_EDIT_QUERY = gql`
  query RequestForEdit($id: [ID!]!) {
    requests(id: $id) {
      ...RequestFieldsForShow
    }
  }
  ${Fragments.RequestFieldsForShow}
`

const LOCAL_STORE_KEY = 'leihs-procure'
const STORE_KEY_FILTERS = `${LOCAL_STORE_KEY}.filters`
const getSavedFilters = () =>
  f.try(() => JSON.parse(window.localStorage.getItem(STORE_KEY_FILTERS)))
const setSavedFilters = o =>
  f.try(() => window.localStorage.setItem(STORE_KEY_FILTERS, JSON.stringify(o)))

class RequestsIndexPage extends React.Component {
  constructor() {
    super()
    this.state = {
      currentFilters: {
        budgetPeriods: [],
        categories: [],
        organizations: [],
        ...getSavedFilters()
      }
    }
    this.onFilterChange = this.onFilterChange.bind(this)
  }
  onFilterChange(filters) {
    this.setState(
      state => ({
        currentFilters: { ...state.filters, ...filters }
      }),
      () => setSavedFilters(this.state.currentFilters)
    )
  }
  onDataRefresh(client) {
    client.resetStore()
  }
  render({ state } = this) {
    return (
      <Query query={FILTERS_QUERY} notifyOnNetworkStatusChange>
        {filtersQuery => {
          return (
            <Query
              query={REQUESTS_QUERY}
              variables={state.currentFilters}
              notifyOnNetworkStatusChange
            >
              {requestsQuery => {
                const refetchAllData = async () => {
                  await filtersQuery.refetch()
                  await requestsQuery.refetch()
                }
                return (
                  <RequestsListFiltered
                    currentFilters={state.currentFilters}
                    onFilterChange={this.onFilterChange}
                    filters={filtersQuery}
                    requestsQuery={requestsQuery}
                    editQuery={REQUEST_EDIT_QUERY}
                    refetchAllData={refetchAllData}
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
