import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'
import { DateTime } from 'luxon'
import { Collapse, FormGroup, Select, ControlledForm } from './Bootstrap'

// import MultiSelect from './Bootstrap/MultiSelect'
import { MainWithSidebar } from './Layout'
import Icon from './Icons'
import Loading from './Loading'
import RequestLine from './RequestLine'
import { ErrorPanel } from './Error'
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
    <div className="h-100 p-3 bg-light">
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
    <Collapse id={'bp' + budgetPeriod.id}>
      {({ isOpen, toggleOpen, togglerProps, collapsedProps, Caret }) => (
        <div className={cx('card mb-3')}>
          <div
            className={cx('card-header pl-2', {
              'border-bottom-0': !isOpen,
              'bg-transparent': !isPast,
              'text-muted': isPast,
              'text-success': isRequesting,
              'text-warning': isInspecting
            })}
            {...togglerProps}
          >
            <h4 className="mb-0 mr-2 d-inline-block">
              <Caret spaced />
              {budgetPeriod.name}
            </h4>
            <Icon.InspectionDate className="mr-2" />
            {inspectStartDate.toLocaleString()}
            <Icon.BudgetPeriod className="mr-2 ml-2" />
            {endDate.toLocaleString()}
          </div>
          {isOpen && (
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
        <li
          className={cx('list-group-item', { disabled: !canToggle })}
          {...togglerProps}
        >
          <h5 className="mb-0">
            <Caret spaced />
            <ImageThumbnail imageUrl={category.image_url} />
            {category.name}
          </h5>
        </li>
        {isOpen && (
          <li className="list-group-item p-0" {...collapsedProps}>
            <ul className="list-group list-group-flush">{props.children}</ul>
          </li>
        )}
      </F>
    )}
  </Collapse>
)

const SubCategoryList = ({ category, requestCount, ...props }) => (
  <Collapse id={'bp' + category.id} canToggle={requestCount > 0}>
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
          className={cx('list-group-item', { disabled: !canToggle })}
          {...togglerProps}
        >
          <h6 className="mb-0">
            <Caret spaced />
            {category.name} <span>({requestCount})</span>
          </h6>
        </li>
        {isOpen && (
          <li className="list-group-item p-0" {...collapsedProps}>
            {props.children}
          </li>
        )}
      </F>
    )}
  </Collapse>
)

// FIXME: remove this when MainCategory.categories scope is fixed
function tmpCleanupCategories(mainCategories) {
  return mainCategories.map(mainCat => ({
    ...mainCat,
    categories: mainCat.categories.filter(
      subCat => subCat.main_category_id === mainCat.id
    )
  }))
}

const RequestsList = ({ requestsQuery: { loading, error, data } }) => {
  if (loading) return <Loading />
  if (error) return <ErrorPanel error={error} />

  const budgetPeriods = data.budget_periods
  const categories = tmpCleanupCategories(data.main_categories)
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
                      <RequestLine
                        key={r.id}
                        fields={r}
                        className={cx('row mx-0 py-2', {
                          'border-bottom': i + 1 < reqs.length
                        })}
                      />
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
