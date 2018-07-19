import React from 'react'
import f from 'lodash'
// import x from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from '../graphql-fragments'
// import Loading from '../components/Loading'
// import { ErrorPanel } from '../components/Error'
import RequestsDashboard from '../components/RequestsDashboard'

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

const FILTERS_QUERY = gql`
  query RequestFilters {
    budget_periods {
      id
      name
    }
    main_categories {
      id
      name
      categories {
        id
        name
      }
    }

    organizations(root_only: true) {
      ...OrgProps
      organizations {
        ...OrgProps
      }
    }

    # priorities {
    #   index
    #   name
    # }
  }

  fragment OrgProps on Organization {
    id
    name
    shortname
  }
`

const REQUESTS_QUERY = gql`
  query RequestsIndexFiltered(
    $budgetPeriods: [ID!]
    $categories: [ID!]
    $search: String
    $organizations: [ID!]
    $priority: [Priority!]
    $inspectory_priority: [InspectorPriority!]
    $onlyOwnRequests: Boolean
  ) {
    budget_periods(id: $budgetPeriods) {
      id
      name
      inspection_start_date
      end_date

      main_categories {
        id
        name
        image_url

        categories(id: $categories) {
          id
          name

          requests(
            search: $search
            organization_id: $organizations
            priority: $priority
            inspectory_priority: $inspectory_priority
            requested_by_auth_user: $onlyOwnRequests
          ) {
            ...RequestFieldsForIndex
          }
        }
      }
    }
  }
  ${Fragments.RequestFieldsForIndex}
`

// TODO: modularize `storageFactory`
const LOCAL_STORE_KEY = 'leihs-procure'
const storageFactory = ({ KEY }) => {
  return {
    get: () => f.try(() => JSON.parse(window.localStorage.getItem(KEY))),
    set: o => f.try(() => window.localStorage.setItem(KEY, JSON.stringify(o)))
  }
}
const userSavedFilters = storageFactory({ KEY: `${LOCAL_STORE_KEY}.filters` })
const savedPanelTree = storageFactory({ KEY: `${LOCAL_STORE_KEY}.panelTree` })

const viewModes = ['tree', 'table']

class RequestsIndexPage extends React.Component {
  constructor() {
    super()
    this.state = {
      viewMode: viewModes[0],
      openPanels: {
        cats: [],
        ...savedPanelTree.get()
      },
      currentFilters: {
        search: '',
        budgetPeriods: [],
        categories: [],
        organizations: [],
        priority: [],
        inspectory_priority: [],
        ...userSavedFilters.get()
      }
    }
    this.onFilterChange = this.onFilterChange.bind(this)
    this.onPanelToggle = this.onPanelToggle.bind(this)
    this.onSetViewMode = this.onSetViewMode.bind(this)
  }
  onPanelToggle(isOpen, id, key = 'cats') {
    const current = this.state.openPanels[key]
    const list = isOpen ? current.concat(id) : current.filter(i => i !== id)
    this.setState(
      state => ({
        openPanels: { ...state.openPanels, [key]: list }
      }),
      () => savedPanelTree.set(this.state.openPanels)
    )
  }
  onSetViewMode(viewMode) {
    if (f.includes([], viewMode)) {
      throw new Error(`Invalid viewMode! '${viewMode}'`)
    }
    this.setState({ viewMode })
  }
  onFilterChange(filters) {
    this.setState(
      state => ({
        currentFilters: { ...state.filters, ...filters }
      }),
      () => userSavedFilters.set(this.state.currentFilters)
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
                  <RequestsDashboard
                    viewMode={state.viewMode}
                    currentFilters={state.currentFilters}
                    onFilterChange={this.onFilterChange}
                    filters={filtersQuery}
                    requestsQuery={requestsQuery}
                    refetchAllData={refetchAllData}
                    openPanels={state.openPanels}
                    onPanelToggle={this.onPanelToggle}
                    onSetViewMode={this.onSetViewMode}
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
