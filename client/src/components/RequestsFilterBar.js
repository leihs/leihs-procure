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

import GroupedSelect from './Bootstrap/MultiSelect'
import { budgetPeriodDates } from './decorators'
import * as CONSTANTS from '../constants'
import t from '../locale/translate'
// import Icon from './Icons'
import Loading from './Loading'
import { ErrorPanel } from './Error'
import RequestStateBadge from './RequestStateBadge'
import CurrentUser from '../containers/CurrentUserProvider'

// import logger from 'debug'
// const log = logger('app:ui:RequestsListFiltered')

const FilterBar = ({
  filters: { loading, error, data, networkStatus, ...restFilters },
  currentFilters,
  onFilterChange,
  ...rest
}) => (
  <CurrentUser>
    {me => {
      const initialLoading = loading && networkStatus < 2

      const content = () => {
        if (initialLoading) return <Loading />
        if (error) {
          return <ErrorPanel error={error} data={data} />
        }
        return (
          <Filters
            me={me}
            data={data}
            current={currentFilters}
            onChange={onFilterChange}
          />
        )
      }

      return (
        <div className="h-100 p-3 bg-light mh-md-100vh">
          <h5>{t('dashboard.filters_title')}</h5>
          {content()}
        </div>
      )
    }}
  </CurrentUser>
)

export default FilterBar

const Filters = ({ me, data, current, onChange }) => {
  const available = {
    budgetPeriods: data.budget_periods.map(({ id, name }) => ({
      value: id,
      label: name
    })),
    categories: data.main_categories.map(({ id, name, categories }) => ({
      label: name,
      options: categories.map(({ id, name }) => ({ value: id, label: name }))
    })),

    organizations: data.organizations.map(({ id, name, organizations }) => ({
      label: name,
      options: organizations.map(({ id, name }) => ({ value: id, label: name }))
    })),

    priority: CONSTANTS.REQUEST_PRIORITIES.map(value => ({
      value,
      label: t(`priority_label_${value}`)
    })),

    inspector_priority: CONSTANTS.REQUEST_INSPECTOR_PRIORITIES.map(value => ({
      value,
      label: t(`inspector_priority_label_${value}`)
    })),

    state: CONSTANTS.REQUEST_STATES.map(value => ({
      value,
      label: t(`request_state_label_${value}`)
    }))
  }

  const allowed = {
    search: true,
    budgetPeriods: true,
    categories: true,
    organizations: !me.roles.isOnlyRequester,
    onlyOwnRequests: !me.roles.isOnlyRequester,
    onlyCategoriesWithRequests: true,
    priority: true,
    inspector_priority: !me.roles.isOnlyRequester,
    state: true
  }

  const defaultFilters = f.pick(
    {
      ...f.fromPairs(
        Object.keys(available).map(key => {
          const values = f.flatMap(
            available[key],
            ({ value, options }) => (value ? value : f.map(options, 'value'))
          )
          return [key, values]
        })
      ),
      budgetPeriods: data.budget_periods
        .filter(bp => {
          const b = budgetPeriodDates(bp)
          if (me.roles.isRequester && b.isRequesting) return true
          if (me.roles.isInspector && b.isInspecting) return true
          return false
        })
        .map(({ id }) => id),
      search: null,
      onlyOwnRequests: false,
      onlyCategoriesWithRequests: true
    },
    f.keys(allowed)
  )

  return (
    <StatefulForm
      idPrefix="requests_filter"
      values={current}
      onChange={onChange}
    >
      {({ fields, formPropsFor, setValue, setValues }) => {
        const selectDefaultFilters = () => setValues(defaultFilters)

        return (
          <F>
            <FormGroup>
              <Button
                size="sm"
                color="link"
                cls="pl-0"
                onClick={selectDefaultFilters}
              >
                {t('dashboard.reset_filters')}
              </Button>
            </FormGroup>

            {allowed.search && (
              <FormGroup label={t('dashboard.filter_titles.search')}>
                <InputText {...formPropsFor('search')} />
              </FormGroup>
            )}

            {allowed.budgetPeriods && (
              <FormGroup label={t('dashboard.filter_titles.budget_periods')}>
                <Select
                  {...formPropsFor('budgetPeriods')}
                  multiple
                  emptyOption={false}
                  options={available.budgetPeriods}
                />
              </FormGroup>
            )}

            {allowed.categories && (
              <FormGroup label={t('dashboard.filter_titles.categories')}>
                <GroupedSelect
                  {...formPropsFor('categories')}
                  multiple
                  size="sm"
                  block
                  options={available.categories}
                />
              </FormGroup>
            )}

            {allowed.onlyOwnRequests &&
              allowed.onlyCategoriesWithRequests && (
                <FormGroup
                  label={t('dashboard.filter_titles.special')}
                  cls="mb-0"
                >
                  {allowed.onlyOwnRequests && (
                    <FormField
                      {...formPropsFor('onlyOwnRequests')}
                      type="checkbox"
                      inputLabel={t(
                        'dashboard.filter_titles.special_only_own_requests'
                      )}
                      label=""
                      hideLabel
                    />
                  )}
                  {allowed.onlyCategoriesWithRequests && (
                    <FormField
                      {...formPropsFor('onlyCategoriesWithRequests')}
                      type="checkbox"
                      inputLabel={t(
                        'dashboard.filter_titles.special_only_categories_with_requests'
                      )}
                      label=""
                      hideLabel
                    />
                  )}
                </FormGroup>
              )}

            {allowed.organizations && (
              <FormGroup label={t('dashboard.filter_titles.orgs')}>
                <GroupedSelect
                  {...formPropsFor('organizations')}
                  size="sm"
                  block
                  multiple
                  options={available.organizations}
                />
              </FormGroup>
            )}

            {allowed.priority && (
              <FormGroup label={t('dashboard.filter_titles.prio')}>
                <Select
                  {...formPropsFor('priority')}
                  multiple
                  emptyOption={false}
                  options={available.priority}
                />
              </FormGroup>
            )}

            {allowed.inspector_priority && (
              <FormGroup label={t('dashboard.filter_titles.prio_insp')}>
                <Select
                  {...formPropsFor('inspector_priority')}
                  multiple
                  emptyOption={false}
                  options={available.inspector_priority}
                />
              </FormGroup>
            )}

            <Let field={formPropsFor('state')}>
              {({ field }) => (
                <FormGroup label={t('dashboard.filter_titles.status')}>
                  {available.state &&
                    available.state.map(({ value, label }) => (
                      <F key={value}>
                        <div className="custom-control custom-checkbox">
                          <input
                            type="checkbox"
                            className="custom-control-input"
                            {...field}
                            id={field.id + value}
                            value={value}
                            checked={f.include(field.value, value)}
                            onChange={e => {
                              const add = e.target.checked
                              const values = field.value || []
                              setValue(
                                'state',
                                f.uniq(
                                  add
                                    ? [...values, value]
                                    : f.without(values, value)
                                )
                              )
                            }}
                          />
                          <label
                            className="custom-control-label"
                            htmlFor={field.id + value}
                          >
                            <RequestStateBadge state={value} />
                          </label>
                        </div>
                      </F>
                    ))}
                </FormGroup>
              )}
            </Let>

            {window.isDebug && <pre>{JSON.stringify(fields, 0, 2)}</pre>}
          </F>
        )
      }}
    </StatefulForm>
  )
}

const Let = ({ children, ...props }) => children(props)
