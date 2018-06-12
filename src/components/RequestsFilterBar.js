import React, { Fragment as F } from 'react'
// import cx from 'classnames'
import f from 'lodash'

import { Button, FormField, FormGroup, StatefulForm, Select } from './Bootstrap'

// import Icon from './Icons'
import Loading from './Loading'
import { ErrorPanel } from './Error'
// import logger from 'debug'
// const log = logger('app:ui:RequestsListFiltered')

const FilterBar = ({
  filters: { loading, error, data, ...restFilters },
  currentFilters,
  onFilterChange,
  ...rest
}) => {
  const content = () => {
    if (loading) return <Loading />
    if (error) {
      return <ErrorPanel error={error} />
    }
    return (
      <Filters data={data} current={currentFilters} onChange={onFilterChange} />
    )
  }

  return (
    <div className="h-100 p-3 bg-light" style={{ minHeight: '100vh' }}>
      <h5>Filters</h5>
      {content()}
    </div>
  )
}

export default FilterBar

const Filters = ({ data, current, onChange }) => {
  const available = {
    budgetPeriods: f
      .sortBy(data.budget_periods, 'name')
      .map(({ id, name }) => ({ value: id, label: name })),
    categories: f
      .sortBy(data.categories, 'name')
      .map(({ id, name }) => ({ value: id, label: name })),
    organizations: f
      .sortBy(data.organizations, 'name')
      .map(({ id, name }) => ({ value: id, label: name }))
  }

  return (
    <StatefulForm
      idPrefix="requests_filter"
      values={current}
      onChange={onChange}
    >
      {({ formPropsFor, setValue }) => {
        const selectAllFilters = () => {
          Object.keys(available).forEach(k =>
            setValue(k, f.map(available[k], 'value'))
          )
        }

        return (
          <F>
            <FormGroup>
              <Button
                size="sm"
                color="link"
                cls="pl-0"
                onClick={selectAllFilters}
              >
                select all
              </Button>
            </FormGroup>
            <FormGroup label={'Budgetperioden'}>
              <Select
                {...formPropsFor('budgetPeriods')}
                multiple
                emptyOption={false}
                options={available.budgetPeriods}
              />
            </FormGroup>
            <FormGroup label={'Kategorien'}>
              <Select
                {...formPropsFor('categories')}
                multiple
                emptyOption={false}
                options={available.categories}
              />
            </FormGroup>
            <FormGroup label={'Organisationen'}>
              <Select
                {...formPropsFor('organizations')}
                multiple
                emptyOption={false}
                options={available.organizations}
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
    </StatefulForm>
  )
}
