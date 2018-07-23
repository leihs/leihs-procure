import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'
import { DateTime } from 'luxon'

import {
  Row,
  Col,
  Button,
  // ButtonGroup,
  // UncontrolledDropdown,
  // DropdownToggle,
  // DropdownMenu,
  // DropdownItem,
  Collapsing,
  Tooltipped
} from './Bootstrap'

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

  const requests = f.flatMap(
    f.flatMap(
      f.flatMap(f.get(requestsQuery, 'data.budget_periods'), 'main_categories'),
      'categories'
    ),
    'requests'
  )

  const pageHeader = (
    <Row>
      <Col>
        <h4>
          {requestsQuery.loading || !requestsQuery.data
            ? ' '
            : `${requests.length || 0} Requests`}
        </h4>
      </Col>
      <Col xs="1" cls="text-right">
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

      <RequestsTree
        requestsQuery={requestsQuery}
        refetchAllData={refetchAllData}
        openPanels={props.openPanels}
        onPanelToggle={props.onPanelToggle}
        doChangeRequestCategory={props.doChangeRequestCategory}
        doChangeBudgetPeriod={props.doChangeBudgetPeriod}
        doDeleteRequest={props.doDeleteRequest}
        editQuery={props.editQuery} //tmp?
        filters={props.currentFilters} // tmp
      />
    </MainWithSidebar>
  )
}

export default RequestsDashboard

const RequestsTree = ({
  requestsQuery: { loading, error, data, networkStatus },
  editQuery,
  filters,
  refetchAllData,
  openPanels,
  onPanelToggle,
  doChangeRequestCategory,
  doChangeBudgetPeriod,
  doDeleteRequest
}) => {
  if (loading) return <Loading size="1" />
  if (error) return <ErrorPanel error={error} data={data} />

  return data.budget_periods.map(b => (
    <BudgetPeriodCard key={b.id} budgetPeriod={b}>
      {b.main_categories.map(cat => {
        const subCatReqs = f.flatMap(f.get(cat, 'categories'), 'requests')
        if (filters.onlyCategoriesWithRequests && f.isEmpty(subCatReqs)) {
          return false
        }
        return (
          <CategoryLine
            key={cat.id}
            category={cat}
            requestCount={subCatReqs.length}
            isOpen={openPanels.cats.includes(cat.id)}
            onToggle={isOpen => onPanelToggle(isOpen, cat.id)}
          >
            {cat.categories.map(sc => {
              const reqs = sc.requests
              if (filters.onlyCategoriesWithRequests && f.isEmpty(reqs)) {
                return false
              }
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
                        <RequestLine
                          request={r}
                          editQuery={editQuery}
                          doChangeRequestCategory={doChangeRequestCategory}
                          doChangeBudgetPeriod={doChangeBudgetPeriod}
                          doDeleteRequest={doDeleteRequest}
                        />
                      </div>
                    </F>
                  ))}
                </SubCategoryLine>
              )
            })}
          </CategoryLine>
        )
      })}
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
    <Collapsing id={`bp_${budgetPeriod.id}`} startOpen>
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
            <Tooltipped text="Antragsphase bis">
              <span
                id={`inspectStartDate_tt_${budgetPeriod.id}`}
                className={cx('mr-3', { 'text-success': isRequesting })}
              >
                <Icon.RequestingPhase className="mr-2" />
                {inspectStartDate.toLocaleString()}
              </span>
            </Tooltipped>

            <Tooltipped text="Inspektionsphase bis">
              <span
                id={`endDate_tt_${budgetPeriod.id}`}
                className={cx({ 'text-success': isInspecting })}
              >
                <Icon.InspectionPhase className="mr-2" />
                {endDate.toLocaleString()}
              </span>
            </Tooltipped>
          </div>
          {isOpen &&
            props.children && (
              <ul className="list-group list-group-flush" {...collapsedProps}>
                {props.children}
              </ul>
            )}
        </div>
      )}
    </Collapsing>
  )
}

const CategoryLine = ({
  category,
  requestCount,
  canToggle,
  isOpen,
  onToggle,
  ...props
}) => (
  <Collapsing
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
            {category.name} <small>({requestCount})</small>
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
  </Collapsing>
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
    <Collapsing
      id={'bp' + category.id}
      canToggle={isOpen || requestCount > 0}
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
    </Collapsing>
  )
}
