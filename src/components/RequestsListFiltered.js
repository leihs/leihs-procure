import React, { Fragment as F } from 'react'
import f from 'lodash'

import { Collapse, FormGroup, Select } from './Bootstrap'
import ControlledForm from './ControlledForm'
// import MultiSelect from './MultiSelect'
import { MainWithSidebar } from './Layout'
// import Icon from './Icons'
import Loading from './Loading'
import RequestLine from './RequestLine'
import { ErrorPanel } from './Error'
import logger from 'debug'
// const log = logger('app:ui:RequestsListFiltered')

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
    <RequestsList requestsQuery={props.requestsQuery} />
  </MainWithSidebar>
)

export default RequestsIndex

const FilterBar = ({
  filters: { loading, error, data, ...restFilters },
  currentFilters,
  onFilterChange,
  ...restArgs
}) => {
  if (loading) return <Loading />
  if (error) {
    return <ErrorPanel error={error} />
  }

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
                <Select
                  {...formPropsFor('budgetPeriods')}
                  emptyOption={false}
                  options={f
                    .sortBy(available.budgetPeriods, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormGroup>
              <FormGroup label={'Kategorien'}>
                <Select
                  {...formPropsFor('categories')}
                  emptyOption={false}
                  options={f
                    .sortBy(available.categories, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormGroup>
              <FormGroup label={'Organisationen'}>
                <Select
                  {...formPropsFor('organizations')}
                  emptyOption={false}
                  options={f
                    .sortBy(available.organizations, 'name')
                    .map(({ id, name }) => ({ value: id, label: name }))}
                />
              </FormGroup>
              {/* <MultiSelect
                id="foo"
                name="foo"
                values={[{ value: '1', label: 'one' }]}
              /> */}
            </F>
          )
        }}
      </ControlledForm>
    </div>
  )
}

const BudgetPeriodCard = ({ budgetPeriod, ...props }) => (
  <Collapse id={'bp' + budgetPeriod.id}>
    {({ isOpen, toggleOpen, togglerProps, collapsedProps, Caret }) => (
      <div className="card mb-3">
        <div className="card-header" {...togglerProps}>
          <h4>
            <Caret className="mr-2" />
            {budgetPeriod.name}
          </h4>
        </div>
        {isOpen && <div {...collapsedProps}>{props.children}</div>}
      </div>
    )}
  </Collapse>
)

const CategoryListGroup = ({ category, canToggle, ...props }) => (
  <Collapse id={'bp' + category.id} canToggle={canToggle}>
    {({
      isOpen,
      canToggle,
      toggleOpen,
      togglerProps,
      collapsedProps,
      Caret
    }) => (
      <F>
        <div className="card-header" {...togglerProps}>
          <h5>
            {canToggle && <Caret className="mr-2" />}
            {category.name}
          </h5>
        </div>
        {isOpen && (
          <ul className="list-group list-group-flush">
            <li className="list-group-item" {...collapsedProps}>
              {props.children}
            </li>
          </ul>
        )}
      </F>
    )}
  </Collapse>
)

const RequestsList = ({ requestsQuery: { loading, error, data } }) => {
  if (loading) return <Loading />
  if (error) return <ErrorPanel error={error} />

  const budgetPeriods = data.budget_periods
  const categories = data.main_categories
  const requests = data.requests
  const groupedRequests = f.groupBy(
    requests,
    // custom key to quickly find later, where we map using those groups:
    r => `${r.budget_period.id}|${r.category.id}`
  )

  return (
    <F>
      <h4>{requests.length} Requests</h4>

      {budgetPeriods.map(b => (
        <BudgetPeriodCard key={b.id} budgetPeriod={b}>
          {categories.map(cat => (
            <CategoryListGroup key={cat.id} category={cat}>
              {cat.categories.map(sc => {
                const reqs = groupedRequests[`${b.id}|${sc.id}`]
                return (
                  <CategoryListGroup
                    key={sc.id}
                    category={sc}
                    canToggle={!f.isEmpty(reqs)}
                  >
                    {f.map(reqs, r => <RequestLine key={r.id} fields={r} />)}
                  </CategoryListGroup>
                )
              })}
            </CategoryListGroup>
          ))}
        </BudgetPeriodCard>
      ))}
    </F>
  )
}
