import React, { Fragment as F } from 'react'
// import cx from 'classnames'
import f from 'lodash'

import {
  Button,
  FormField,
  InputText,
  FormGroup,
  StatefulForm,
  Select
} from './Bootstrap'

// WIP:
import MultiSelect from './Bootstrap/DownshiftMultiSelect'

import * as CONSTANTS from '../constants'
import t from '../locale/translate'
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
      return <ErrorPanel error={error} data={data} />
    }
    return (
      <Filters data={data} current={currentFilters} onChange={onFilterChange} />
    )
  }

  return (
    <div className="h-100 p-3 bg-light mh-md-100vh">
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

    categories: data.main_categories.map(({ id, name, categories }) => ({
      label: name,
      options: f
        .sortBy(categories, 'name')
        .map(({ id, name }) => ({ value: id, label: name }))
    })),

    organizations: f
      .sortBy(data.organizations, 'name')
      .map(({ id, name }) => ({ value: id, label: name })),

    priority: CONSTANTS.REQUEST_PRIORITIES.map(value => ({
      value,
      label: t(`priority_label_${value}`)
    })),

    inspectory_priority: CONSTANTS.REQUEST_INSPECTOR_PRIORITIES.map(value => ({
      value,
      label: t(`inspector_priority_label_${value}`)
    }))
  }

  return (
    <StatefulForm
      idPrefix="requests_filter"
      values={current}
      onChange={onChange}
    >
      {({ fields, formPropsFor, setValue }) => {
        const selectDefaultFilters = () => {
          Object.keys(available).forEach(k =>
            setValue(k, f.map(available[k], 'value'))
          )
          setValue('onlyOwnRequests', false)
          setValue('onlyCategoriesWithRequests', true)
        }

        return (
          <F>
            <FormGroup>
              <Button
                size="sm"
                color="link"
                cls="pl-0"
                onClick={selectDefaultFilters}
              >
                reset filters
              </Button>
            </FormGroup>

            <FormGroup label="Suche">
              <InputText {...formPropsFor('search')} />
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
              {/* <Select
                {...formPropsFor('categories')}
                multiple
                emptyOption={false}
                options={available.categories}
              /> */}
              <MultiSelect
                {...formPropsFor('categories')}
                multiple
                options={available.categories}
              />
            </FormGroup>
            <FormGroup label={'Spezialfilter'}>
              <FormField
                {...formPropsFor('onlyOwnRequests')}
                type="checkbox"
                inputLabel="only own Requests"
                label="only own Requests"
                hideLabel
              />
              <FormField
                {...formPropsFor('onlyCategoriesWithRequests')}
                type="checkbox"
                inputLabel="only Categories with Requests"
                label="only Categories with Requests"
                hideLabel
              />
            </FormGroup>
            <FormGroup label={'Organisationen'}>
              <code>TBD</code>
              {/* FIXME: wait for MultiSelect (can only select non-root!)
              <Select
                {...formPropsFor('organizations')}
                multiple
                emptyOption={false}
                options={available.organizations}
              />
              */}
            </FormGroup>
            <FormGroup label={'Priorität'}>
              <Select
                {...formPropsFor('priority')}
                multiple
                emptyOption={false}
                options={available.priority}
              />
            </FormGroup>
            <FormGroup label={'Priorität des Prüfers'}>
              <Select
                {...formPropsFor('inspectory_priority')}
                multiple
                emptyOption={false}
                options={available.inspectory_priority}
              />
            </FormGroup>
            <FormGroup label={'Status Antrag'}>
              <code>TBD</code>
            </FormGroup>
            {window.isDebug && <pre>{JSON.stringify(fields, 0, 2)}</pre>}
          </F>
        )
      }}
    </StatefulForm>
  )
}
