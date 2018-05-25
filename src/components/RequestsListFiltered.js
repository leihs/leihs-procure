import React, { Fragment as F } from 'react'
import f from 'lodash'

import { FormGroup } from './Bootstrap'
import ControlledForm from './ControlledForm'
import MultiSelect from './MultiSelect'
import { MainWithSidebar } from './Layout'
import Loading from './Loading'
import RequestLine from './RequestLine'
import { ErrorPanel } from './Error'

const FilterBar = ({
  filters: { loading, error, data },
  currentFilters,
  onFilterChange
}) => {
  if (loading) return <Loading />
  if (error) return <ErrorPanel error={error} />

  const available = {
    budgetPeriods: data.budget_periods,
    categories: data.categories,
    organizations: data.organizations
  }

  return (
    <div className="p-3 bg-light">
      <h5>Filters</h5>
      <ControlledForm
        idPrefix="requests_filter"
        values={currentFilters}
        onChange={onFilterChange}
      >
        {({ formPropsFor }) => {
          return (
            <F>
              <FormGroup label={'Budgetperioden'}>
                <MultiSelect
                  {...formPropsFor('budgetPeriods')}
                  values={f
                    .sortBy(available.budgetPeriods, 'name')
                    .map(({ id, name }) => ({ id, label: name }))}
                />
              </FormGroup>
              <FormGroup label={'Kategorien'}>
                <MultiSelect
                  {...formPropsFor('categories')}
                  values={f
                    .sortBy(available.categories, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormGroup>
              <FormGroup label={'Organisationen'}>
                <MultiSelect
                  {...formPropsFor('organizations')}
                  values={f
                    .sortBy(available.organizations, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormGroup>
              {/* <MultiSelect
                id="foo"
                name="foo"
                values={[{ id: '1', label: 'one' }]}
              /> */}
            </F>
          )
        }}
      </ControlledForm>
    </div>
  )
}

const RequestsList = ({ requests: { loading, error, data } }) => {
  if (loading) return <Loading />
  if (error) return <ErrorPanel error={error} />
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
    }
  >
    <RequestsList requests={props.requests} />
  </MainWithSidebar>
)

export default RequestsIndex
