import React, { Fragment as F } from 'react'
import f from 'lodash'

import { FormField, Select } from './Bootstrap'
import ControlledForm from './ControlledForm'
import { MainWithSidebar } from './Layout'
import Loading from './Loading'
import RequestLine from './RequestLine'

const FilterBar = ({
  filters: { loading, error, data },
  currentFilters,
  onFilterChange
}) => {
  if (loading) return <Loading />
  if (error)
    return (
      <div>
        <p>Error :(</p>
        <pre>{error.toString()}</pre>
      </div>
    )

  const available = {
    budgetPeriods: data.budget_periods,
    categories: data.categories
  }

  return (
    <div className="pt-2">
      <h5>Filters</h5>
      <ControlledForm
        idPrefix="requests_filter"
        values={currentFilters}
        onChange={onFilterChange}
        render={({ formPropsFor }) => {
          return (
            <F>
              <FormField label={'Budgetperioden'}>
                <Select
                  {...formPropsFor('budgetPeriods')}
                  emptyOption={false}
                  options={f
                    .sortBy(available.budgetPeriods, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormField>
              <FormField label={'Kategorien'}>
                <Select
                  {...formPropsFor('categories')}
                  emptyOption={false}
                  options={f
                    .sortBy(available.categories, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormField>
            </F>
          )
        }}
      />
    </div>
  )
}

const RequestsList = ({ requests: { loading, error, data } }) => {
  if (loading) return <Loading />
  if (error) return <p>Error :(</p>
  const requests = data && data.requests ? data.requests : []
  return (
    <F>
      {' '}
      <h4>{requests.length} Requests</h4>
      {requests.map(r => <RequestLine key={r.id} fields={r} />)}
    </F>
  )
}
const RequestsIndex = props => (
  <MainWithSidebar
    sidebar={
      <FilterBar
        filters={props.filters}
        currentFilters={props.currentFilters}
        onFilterChange={props.onFilterChange}
      />
    }>
    <RequestsList requests={props.requests} />
  </MainWithSidebar>
)

export default RequestsIndex
