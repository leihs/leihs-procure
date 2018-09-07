import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'

import {
  Row,
  Col,
  Button,
  FormField,
  InputText,
  FormGroup,
  StatefulForm,
  Collapsing
} from './Bootstrap'

import MultiSelect from './Bootstrap/MultiSelect'
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

const breakPoint = 'xl' // starting with this size is shown on right side etc

const FilterBar = ({
  filters: { loading, error, data, networkStatus, ...restFilters },
  currentFilters,
  onFilterChange,
  ...rest
}) => (
  <CurrentUser>
    {me => {
      const initialLoading = loading && networkStatus < 2

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
    }}
  </CurrentUser>
)

export default FilterBar

const Filters = ({ me, data, current, onChange }) => {
  const heading = t('dashboard.filters_title')

  const budgetPeriods = data.budget_periods
  const budgetPeriodsPyPhase = f.groupBy(budgetPeriods, bp => {
    const d = budgetPeriodDates(bp)
    return ['isInspecting', 'isRequesting'].filter(k => d[k])[0] || 'isPast'
  })

  const available = {
    budgetPeriods: f.map(budgetPeriodsPyPhase, (bps, phase) => ({
      label: t(`budget_period_filter_label_state.${phase}`),
      options: bps.map(({ id, name }) => ({ value: id, label: name }))
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
      // by default, select everything
      ...f.fromPairs(
        Object.keys(available).map(key => {
          const values = f.flatMap(
            available[key],
            ({ value, options }) => (value ? value : f.map(options, 'value'))
          )
          return [key, values]
        })
      ),
      // only select "current" BudgetPeriods
      budgetPeriods: data.budget_periods
        .filter(bp => {
          const b = budgetPeriodDates(bp)
          if (me.roles.isRequester && b.isRequesting) return true
          if (me.roles.isInspector && b.isInspecting) return true
          return false
        })
        .map(({ id }) => id),
      // specific values:
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
          <Collapsing id="collapse-example-1" startOpen>
            {({ isOpen, togglerProps, collapsedProps, Caret }) => (
              <div className={`h-100 p-3 bg-light mh-${breakPoint}-100vh`}>
                <div
                  className={`d-block d-${breakPoint}-none`}
                  {...togglerProps}
                >
                  <h5>
                    <Caret spaced />
                    {heading}
                  </h5>
                </div>
                <div className={`d-none d-${breakPoint}-block`}>
                  <h5>{heading}</h5>
                </div>

                <div
                  className={cx('d-${breakPoint}-block', { 'd-none': !isOpen })}
                >
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

                    <Row>
                      {allowed.budgetPeriods && (
                        <Col sm cls={`col-${breakPoint}-12`}>
                          <FormGroup
                            label={t('dashboard.filter_titles.budget_periods')}
                          >
                            <MultiSelect
                              {...formPropsFor('budgetPeriods')}
                              withSearch={budgetPeriods.length > 24}
                              multiple
                              size="sm"
                              block
                              options={available.budgetPeriods}
                            />
                          </FormGroup>
                        </Col>
                      )}
                      {allowed.categories && (
                        <Col sm cls={`col-${breakPoint}-12`}>
                          <FormGroup
                            label={t('dashboard.filter_titles.categories')}
                          >
                            <MultiSelect
                              {...formPropsFor('categories')}
                              multiple
                              size="sm"
                              block
                              options={available.categories}
                            />
                          </FormGroup>
                        </Col>
                      )}
                    </Row>

                    <Row>
                      {allowed.onlyOwnRequests &&
                        allowed.onlyCategoriesWithRequests && (
                          <Col sm cls={`col-${breakPoint}-12`}>
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
                                  {...formPropsFor(
                                    'onlyCategoriesWithRequests'
                                  )}
                                  type="checkbox"
                                  inputLabel={t(
                                    'dashboard.filter_titles.special_only_categories_with_requests'
                                  )}
                                  label=""
                                  hideLabel
                                />
                              )}
                            </FormGroup>
                          </Col>
                        )}

                      {allowed.organizations && (
                        <Col sm cls={`col-${breakPoint}-12`}>
                          <FormGroup label={t('dashboard.filter_titles.orgs')}>
                            <MultiSelect
                              {...formPropsFor('organizations')}
                              size="sm"
                              block
                              multiple
                              options={available.organizations}
                            />
                          </FormGroup>
                        </Col>
                      )}
                    </Row>

                    <Row>
                      <Col sm cls={`col-${breakPoint}-12`}>
                        {allowed.priority && (
                          <FormGroup label={t('dashboard.filter_titles.prio')}>
                            <MultiSelect
                              {...formPropsFor('priority')}
                              size="sm"
                              block
                              multiple
                              withSearch={false}
                              options={available.priority}
                            />
                          </FormGroup>
                        )}
                      </Col>
                      <Col sm cls={`col-${breakPoint}-12`}>
                        {allowed.inspector_priority && (
                          <FormGroup
                            label={t('dashboard.filter_titles.prio_insp')}
                          >
                            <MultiSelect
                              {...formPropsFor('inspector_priority')}
                              size="sm"
                              block
                              multiple
                              withSearch={false}
                              options={available.inspector_priority}
                            />
                          </FormGroup>
                        )}
                      </Col>
                    </Row>

                    <Let field={formPropsFor('state')}>
                      {({ field }) => (
                        <FormGroup label={t('dashboard.filter_titles.status')}>
                          {available.state &&
                            available.state.map(({ value, label }) => (
                              <F key={value}>
                                <div
                                  className={cx(
                                    'custom-control custom-checkbox',
                                    'mr-3 d-inline-block',
                                    `mr-${breakPoint}-0 d-${breakPoint}-block`
                                  )}
                                >
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

                    {window.isDebug && (
                      <pre>{JSON.stringify(fields, 0, 2)}</pre>
                    )}
                  </F>
                </div>
              </div>
            )}
          </Collapsing>
        )
      }}
    </StatefulForm>
  )
}

const Let = ({ children, ...props }) => children(props)
