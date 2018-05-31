import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'
import { DateTime } from 'luxon'
import { Collapse, FormGroup, Select, ControlledForm } from './Bootstrap'

// import MultiSelect from './Bootstrap/MultiSelect'
import { MainWithSidebar } from './Layout'
import Icon from './Icons'
import Loading from './Loading'
import { ErrorPanel } from './Error'
import RequestLine from './RequestLine'
import ImageThumbnail from './ImageThumbnail'
// import logger from 'debug'
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
    <RequestsList
      requestsQuery={props.requestsQuery}
      editQuery={props.editQuery} //tmp?
      filters={props.currentFilters} // tmp
    />
  </MainWithSidebar>
)

export default RequestsIndex

const FilterBar = ({
  filters: { loading, error, data, ...restFilters },
  currentFilters,
  onFilterChange,
  ...rest
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
    <div className="h-100 p-3 bg-light o-50 pointer-events-none">
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

const budgetPeriodDates = bp => {
  const now = DateTime.local()
  const inspectStartDate = DateTime.fromISO(bp.inspection_start_date)
  const endDate = DateTime.fromISO(bp.end_date)
  const isPast = endDate <= now
  const isRequesting = !isPast && now <= inspectStartDate
  const isInspecting = !isPast && !isRequesting
  return { inspectStartDate, endDate, isPast, isRequesting, isInspecting }
}

const BudgetPeriodCard = ({ budgetPeriod, ...props }) => {
  const {
    inspectStartDate,
    endDate,
    isPast,
    isRequesting,
    isInspecting
  } = budgetPeriodDates(budgetPeriod)

  return (
    <Collapse id={'bp' + budgetPeriod.id} startOpen>
      {({ isOpen, toggleOpen, togglerProps, collapsedProps, Caret }) => (
        <div className={cx('card mb-3')}>
          <div
            className={cx('card-header cursor-pointer pl-2', {
              'border-bottom-0': !isOpen,
              'bg-transparent': !isPast,
              'text-muted': isPast
            })}
            {...togglerProps}
          >
            <h2 className="mb-0 mr-3 h3 d-inline-block">
              <Caret spaced />
              {budgetPeriod.name}
            </h2>
            <span
              title="Antragsphase bis"
              className={cx('mr-3', { 'text-success': isRequesting })}
            >
              <Icon.RequestingPhase className="mr-2" />
              {inspectStartDate.toLocaleString()}
            </span>

            <span
              title="Inspectionsphase bis"
              className={cx({ 'text-success': isInspecting })}
            >
              <Icon.InspectionPhase className="mr-2" />
              {endDate.toLocaleString()}
            </span>
          </div>
          {isOpen &&
            props.children && (
              <ul className="list-group list-group-flush" {...collapsedProps}>
                {props.children}
              </ul>
            )}
        </div>
      )}
    </Collapse>
  )
}

const CategoryList = ({ category, canToggle, ...props }) => (
  <Collapse id={'bp' + category.id} canToggle={canToggle} startOpen>
    {({
      isOpen,
      canToggle,
      toggleOpen,
      togglerProps,
      collapsedProps,
      Caret
    }) => (
      <F>
        <li
          className={cx('list-group-item ', {
            disabled: !canToggle,
            'cursor-pointer': canToggle
          })}
          {...togglerProps}
        >
          <h5 className="mb-0">
            <Caret spaced />
            <ImageThumbnail imageUrl={category.image_url} />
            {category.name}
          </h5>
        </li>
        {isOpen &&
          props.children && (
            <li className="list-group-item p-0" {...collapsedProps}>
              <ul className="list-group list-group-flush">{props.children}</ul>
            </li>
          )}
      </F>
    )}
  </Collapse>
)

const SubCategoryList = ({ category, requestCount, ...props }) => {
  const showChildren = (isOpen, children) =>
    !!isOpen && React.Children.count(children) > 0

  return (
    <Collapse id={'bp' + category.id} canToggle={requestCount > 0} startOpen>
      {({
        isOpen,
        canToggle,
        toggleOpen,
        togglerProps,
        collapsedProps,
        Caret
      }) => (
        <F>
          <li
            className={cx('list-group-item', {
              disabled: !canToggle,
              'cursor-pointer': canToggle
            })}
            {...togglerProps}
          >
            <h6 className="mb-0">
              <Caret spaced />
              {category.name} <span>({requestCount})</span>
            </h6>
          </li>
          {showChildren(isOpen, props.children) && (
            <li
              className="list-group-item p-0 ui-subcat-items"
              {...collapsedProps}
            >
              {props.children}
            </li>
          )}
        </F>
      )}
    </Collapse>
  )
}

// FIXME: remove this when MainCategory.categories scope is fixed
function tmpCleanupCategories(mainCategories) {
  return mainCategories.map(mainCat => ({
    ...mainCat,
    categories: mainCat.categories.filter(
      subCat => subCat.main_category_id === mainCat.id
    )
  }))
}

// FIXME: remove this budgetperiods query can be filtered by id
function tmpFilterBudgetPeriods(periods, filters) {
  if (!filters.budgetPeriods) return periods
  return periods.filter(p => filters.budgetPeriods.indexOf(p.id) !== -1)
}

// FIXME: remove this main_categories query can be filtered by id
function tmpFilterMainCategories(categories, filters) {
  if (!filters.categories) return categories
  return categories.filter(c => filters.categories.indexOf(c.id) !== -1)
}

const RequestsList = ({
  requestsQuery: { loading, error, data },
  editQuery,
  filters
}) => {
  if (loading) return <Loading />
  if (error) return <ErrorPanel error={error} />

  const budgetPeriods = tmpFilterBudgetPeriods(data.budget_periods, filters)
  const categories = tmpCleanupCategories(
    tmpFilterMainCategories(data.main_categories, filters)
  )
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
            <CategoryList key={cat.id} category={cat}>
              {cat.categories.map(sc => {
                const reqs = groupedRequests[`${b.id}|${sc.id}`] || []
                return (
                  <SubCategoryList
                    key={sc.id}
                    category={sc}
                    requestCount={reqs.length}
                  >
                    {f.map(reqs, (r, i) => (
                      <F key={r.id}>
                        <div
                          className={cx({
                            'border-bottom': i + 1 < reqs.length // not if last
                          })}
                        >
                          <RequestLine request={r} editQuery={editQuery} />
                        </div>
                      </F>
                    ))}
                  </SubCategoryList>
                )
              })}
            </CategoryList>
          ))}
        </BudgetPeriodCard>
      ))}
    </F>
  )
}
