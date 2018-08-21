import React from 'react'
import f from 'lodash'
// import x from 'lodash'
import { Query, ApolloConsumer } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from '../graphql-fragments'
// import Loading from '../components/Loading'
// import { ErrorPanel } from '../components/Error'
import RequestsDashboard from '../components/RequestsDashboard'

const FILTERS_QUERY = gql`
  query RequestFilters {
    budget_periods {
      id
      name
      inspection_start_date
      end_date
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
    $inspector_priority: [InspectorPriority!]
    $onlyOwnRequests: Boolean
  ) {
    budget_periods(id: $budgetPeriods) {
      id
      name
      inspection_start_date
      end_date
      total_price_cents

      main_categories {
        id
        name
        image_url
        total_price_cents

        categories(id: $categories) {
          id
          name
          total_price_cents

          requests(
            search: $search
            organization_id: $organizations
            priority: $priority
            inspector_priority: $inspector_priority
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

const doChangeRequestCategory = (client, requestId, newCatId, callback) => {
  const CHANGE_REQUEST_CATEGORY_MUTATION = gql`
    mutation changeRequestCategory($input: RequestCategoryInput!) {
      # NOTE: we dont need any return data bc we refetch anyways
      change_request_category(input_data: $input) {
        id
      }
    }
  `

  client
    .mutate({
      mutation: CHANGE_REQUEST_CATEGORY_MUTATION,
      variables: { input: { id: requestId, category: newCatId } },
      update: callback
    })
    .catch(error => window.alert(error))
}

const doChangeBudgetPeriod = (client, requestId, newBudPeriodId, callback) => {
  const CHANGE_BUDGET_PERIOD_MUTATION = gql`
    mutation changeBudgetPeriod($input: RequestBudgetPeriodInput!) {
      # NOTE: we dont need any return data bc we refetch anyways
      change_request_budget_period(input_data: $input) {
        id
      }
    }
  `

  client
    .mutate({
      mutation: CHANGE_BUDGET_PERIOD_MUTATION,
      variables: { input: { id: requestId, budget_period: newBudPeriodId } },
      update: callback
    })
    .catch(error => window.alert(error))
}

const doDeleteRequest = (client, request, callback) => {
  const DELETE_REQUEST_MUTATION = gql`
    mutation changeRequestCategory($input: DeleteRequestInput) {
      delete_request(input_data: $input)
    }
  `

  if (!window.confirm('Delete?')) return

  client
    .mutate({
      mutation: DELETE_REQUEST_MUTATION,
      variables: { input: { id: request.id } },
      update: (cache, response) => {
        if (!response.data.delete_request) {
          window.alert('Could not delete!')
        }
        // NOTE: manual store update doesnt work yet, but data is this:
        // const updatedData = {
        //   budget_periods: currentData.budget_periods.map(bp => ({
        //     ...bp,
        //     main_categories: bp.main_categories.map(mc => ({
        //       ...mc,
        //       categories: mc.categories.map(sc => ({
        //         ...sc,
        //         requests: sc.requests.filter(r => r.id !== request.id)
        //       }))
        //     }))
        //   }))
        // }
        // cache.writeQuery({
        //   query: REQUESTS_QUERY,
        //   data: updatedData
        // })
        callback() // does a full reload :/
      }
    })
    .catch(error => window.alert(error))
}

// TODO: modularize `storageFactory`
const LOCAL_STORE_KEY = 'leihs-procure'
const storageFactory = ({ KEY }) => {
  return {
    get: () => f.try(() => JSON.parse(window.localStorage.getItem(KEY))),
    set: o => f.try(() => window.localStorage.setItem(KEY, JSON.stringify(o)))
  }
}
// TODO: use user.savedFilters from server, scope cache by user.id!!!
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
        inspector_priority: [],
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
      <ApolloConsumer>
        {client => (
          <Query
            query={FILTERS_QUERY}
            fetchPolicy="cache-and-network"
            notifyOnNetworkStatusChange
          >
            {filtersQuery => {
              return (
                <Query
                  query={REQUESTS_QUERY}
                  variables={state.currentFilters}
                  fetchPolicy="cache-and-network"
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
                        doDeleteRequest={r =>
                          doDeleteRequest(client, r, refetchAllData)
                        }
                        doChangeRequestCategory={(r, categoryId) =>
                          doChangeRequestCategory(
                            client,
                            r,
                            categoryId,
                            refetchAllData
                          )
                        }
                        doChangeBudgetPeriod={(r, budgetPerId) =>
                          doChangeBudgetPeriod(
                            client,
                            r,
                            budgetPerId,
                            refetchAllData
                          )
                        }
                      />
                    )
                  }}
                </Query>
              )
            }}
          </Query>
        )}
      </ApolloConsumer>
    )
  }
}

export default RequestsIndexPage
