import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'
import { DateTime } from 'luxon'

import { Row, Col, Button, ButtonGroup, Collapse } from './Bootstrap'

// import MultiSelect from './Bootstrap/MultiSelect'
import { MainWithSidebar } from './Layout'
import Icon from './Icons'
import Loading from './Loading'
import { ErrorPanel } from './Error'
import RequestLine from './RequestLine'
import ImageThumbnail from './ImageThumbnail'

import FilterBar from './RequestsFilterBar'
// import logger from 'debug'
// const log = logger('app:ui:RequestsTreeFiltered')

const RequestsDashboard = props => {
  const { requestsQuery, refetchAllData } = props
  const pageHeader = (
    <Row>
      <Col>
        <h4>
          {f.get(props.requestsQuery.data, 'requests.length') || 0} Requests
        </h4>
      </Col>
      <Col xs="1" cls="text-right">
        <ButtonGroup size="sm">
          {['tree', 'table'].map(m => (
            <Button
              key={m}
              size="sm"
              outline
              color="secondary"
              active={props.viewMode === m}
              onClick={e => props.onSetViewMode(m)}
            >
              {m}
            </Button>
          ))}
        </ButtonGroup>
        <Button color="link" title="refresh data" onClick={refetchAllData}>
          <Icon.Reload spin={requestsQuery.loading} />
        </Button>
      </Col>
    </Row>
  )

  return (
    <MainWithSidebar
      sidebar={
        <FilterBar
          filters={props.filters}
          currentFilters={props.currentFilters}
          onFilterChange={props.onFilterChange}
        />
      }
    >
      {pageHeader}

      {props.viewMode === 'table' ? (
        <RequestsTable
          requestsQuery={requestsQuery}
          refetchAllData={refetchAllData}
          editQuery={props.editQuery} //tmp?
        />
      ) : (
        <RequestsTree
          requestsQuery={requestsQuery}
          refetchAllData={refetchAllData}
          openPanels={props.openPanels}
          onPanelToggle={props.onPanelToggle}
          editQuery={props.editQuery} //tmp?
          filters={props.currentFilters} // tmp
        />
      )}
    </MainWithSidebar>
  )
}

export default RequestsDashboard

// FIXME: remove this budgetperiods query can be filtered by id
function tmpFilterBudgetPeriods(periods, filters) {
  if (!filters.budgetPeriods) return periods
  return periods.filter(p => filters.budgetPeriods.indexOf(p.id) !== -1)
}

const RequestsTable = ({
  requestsQuery: { loading, error, data },
  editQuery,
  filters,
  refetchAllData,
  openPanels,
  onPanelToggle
}) => {
  if (loading) return <Loading size="1" />
  if (error) return <ErrorPanel error={error} data={data} />

  const requests = f.sortBy(data.requests, r =>
    [r.budget_period, r.category_id].join('.')
  )

  return f.map(requests, (r, i) => (
    <F key={r.id}>
      <div
        className={cx('border-top', {
          'border-bottom': i + 1 === requests.length // only last
        })}
      >
        <RequestLine request={r} editQuery={editQuery} />
      </div>
    </F>
  ))
}

const RequestsTree = ({
  requestsQuery: { loading, error, data },
  editQuery,
  filters,
  refetchAllData,
  openPanels,
  onPanelToggle
}) => {
  if (loading) return <Loading size="1" />
  if (error) return <ErrorPanel error={error} data={data} />

  const budgetPeriods = tmpFilterBudgetPeriods(data.budget_periods, filters)
  const categories = data.main_categories
  const requests = data.requests
  const groupedRequests = f.groupBy(
    requests,
    // custom key to quickly find later, where we map using those groups:
    r => `${r.budget_period.id}|${r.category.id}`
  )

  return budgetPeriods.map(b => (
    <BudgetPeriodCard key={b.id} budgetPeriod={b}>
      {categories.map(cat => (
        <CategoryLine
          key={cat.id}
          category={cat}
          isOpen={openPanels.cats.includes(cat.id)}
          onToggle={isOpen => onPanelToggle(isOpen, cat.id)}
        >
          {cat.categories.map(sc => {
            const reqs = groupedRequests[`${b.id}|${sc.id}`] || []
            return (
              <SubCategoryLine
                key={sc.id}
                category={sc}
                requestCount={reqs.length}
                isOpen={openPanels.cats.includes(sc.id)}
                onToggle={isOpen => onPanelToggle(isOpen, sc.id)}
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
              </SubCategoryLine>
            )
          })}
        </CategoryLine>
      ))}
    </BudgetPeriodCard>
  ))
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

const CategoryLine = ({ category, canToggle, isOpen, onToggle, ...props }) => (
  <Collapse
    id={'bp' + category.id}
    canToggle={canToggle}
    startOpen={isOpen}
    onChange={({ isOpen }) => onToggle(isOpen)}
  >
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

const SubCategoryLine = ({
  category,
  requestCount,
  isOpen,
  onToggle,
  ...props
}) => {
  const showChildren = (isOpen, children) =>
    !!isOpen && React.Children.count(children) > 0

  return (
    <Collapse
      id={'bp' + category.id}
      canToggle={requestCount > 0}
      startOpen={isOpen}
      onChange={({ isOpen }) => onToggle(isOpen)}
    >
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
