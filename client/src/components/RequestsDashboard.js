import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'
import { Link } from 'react-router-dom'
import { stringify as stringifyQuery } from 'qs'

import t from '../locale/translate'
import { Row, Col, Button, Collapsing, Tooltipped } from './Bootstrap'
import { formatCurrency, budgetPeriodDates } from './decorators'
// import MultiSelect from './Bootstrap/MultiSelect'
import { MainWithSidebar } from './Layout'
import Icon from './Icons'
import Loading from './Loading'
import { ErrorPanel } from './Error'
import RequestLine from './RequestLine'
import ImageThumbnail from './ImageThumbnail'

import CurrentUser from '../containers/CurrentUserProvider'
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
            : `${requests.length || 0} ${
                requests.length === 1
                  ? t('dashboard.requests_title_singular')
                  : t('dashboard.requests_title_plural')
              }`}
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

      <CurrentUser>
        {me => (
          <RequestsTree
            requestsQuery={requestsQuery}
            me={me}
            refetchAllData={refetchAllData}
            openPanels={props.openPanels}
            onPanelToggle={props.onPanelToggle}
            doChangeRequestCategory={props.doChangeRequestCategory}
            doChangeBudgetPeriod={props.doChangeBudgetPeriod}
            doDeleteRequest={props.doDeleteRequest}
            editQuery={props.editQuery} //tmp?
            filters={props.currentFilters} // tmp
          />
        )}
      </CurrentUser>
    </MainWithSidebar>
  )
}

export default RequestsDashboard

const RequestsTree = ({
  requestsQuery: { loading, error, data, networkStatus },
  me,
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
    <BudgetPeriodCard key={b.id} budgetPeriod={b} me={me}>
      {b.main_categories.map(cat => {
        const subCatReqs = f.flatMap(f.get(cat, 'categories'), 'requests')
        if (filters.onlyCategoriesWithRequests && f.isEmpty(subCatReqs)) {
          return false
        }

        return (
          <CategoryLine
            key={cat.id}
            me={me}
            budgetPeriod={b}
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
                  me={me}
                  budgetPeriod={b}
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

const BudgetPeriodCard = ({ budgetPeriod, me, ...props }) => {
  const {
    inspectStartDate,
    endDate,
    isPast,
    isRequesting,
    isInspecting
  } = budgetPeriodDates(budgetPeriod)

  const canRequest =
    (isRequesting && me.roles.isRequester) ||
    (isInspecting && (me.roles.isAdmin || me.roles.isInspector))
  const newRequestBpLink = canRequest && newRequestLink({ budgetPeriod })

  const children = f.some(props.children) ? props.children : false

  return (
    <Collapsing id={`bp_${budgetPeriod.id}`} startOpen>
      {({ isOpen, toggleOpen, togglerProps, collapsedProps, Caret }) => (
        <div className={cx('card mb-3')}>
          <div
            className={cx('card-header cursor-pointer pl-2', {
              'border-bottom-0': !(isOpen && children),
              'bg-transparent': !isPast,
              'text-muted': isPast
            })}
            {...togglerProps}
          >
            <div className="d-flex flex-wrap justify-content-between align-items-baseline">
              <div className="flex-grow-1 flex-sm-grow-0 w-50">
                <h2 className="mb-0 h3 d-inline-block">
                  <Caret spaced />
                  {budgetPeriod.name}
                </h2>

                <div className="d-inline-flex flex-wrap ml-3 mt-2">
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
                      className={cx('mr-3', { 'text-success': isInspecting })}
                    >
                      <Icon.InspectionPhase className="mr-2" />
                      {endDate.toLocaleString()}
                    </span>
                  </Tooltipped>
                </div>
              </div>

              <div className="mr-auto">
                {!!budgetPeriod.total_price_cents && (
                  <Tooltipped text={t('dashboard.bp_total_sum')}>
                    <span className="ml-1" id={`ordqb_tt_${budgetPeriod.id}`}>
                      <Icon.ShoppingCart className="mr-1" />
                      <samp>
                        {formatCurrency(budgetPeriod.total_price_cents)}
                      </samp>
                    </span>
                  </Tooltipped>
                )}
              </div>

              <div className="ml-3 mt-2 mt-md-0">
                {newRequestBpLink && (
                  <Link to={newRequestBpLink}>
                    <Tooltipped text={t('dashboard.create_request_for_bp')}>
                      <Icon.PlusCircle
                        id={`tt_bp_cnr_${budgetPeriod.id}`}
                        size="2x"
                        color="success"
                      />
                    </Tooltipped>
                  </Link>
                )}
              </div>
            </div>
          </div>

          {isOpen &&
            children && (
              <ul
                className="list-group list-group-flush bp-cat-list"
                {...collapsedProps}
              >
                {children}
              </ul>
            )}
        </div>
      )}
    </Collapsing>
  )
}

const CategoryLine = ({
  me,
  budgetPeriod,
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
          <div className="d-flex flex-wrap justify-content-between align-items-baseline">
            <div className="w-50">
              <h5 className="mb-0 mr-3 d-inline-block">
                <Caret spaced />
                <ImageThumbnail imageUrl={category.image_url} />
                {category.name} <small>({requestCount})</small>
              </h5>
            </div>

            <div className="mr-auto">
              {!!category.total_price_cents && (
                <Tooltipped text={t('dashboard.maincat_total_sum')}>
                  <small id={`ordqmc_tt_${category.id}`}>
                    <Icon.ShoppingCart className="mr-1" />
                    <samp>{formatCurrency(category.total_price_cents)}</samp>
                  </small>
                </Tooltipped>
              )}
            </div>

            <div className="ml-3 mt-2 mt-md-0">
              {/* TODO: decide if/how to show these links
              {canRequest && (
                <Link
                  to={newRequestLink({ budgetPeriod, mainCategory: category })}
                >
                  <Tooltipped text={t('dashboard.create_request_for_maincat')}>
                    <Icon.PlusCircle
                      id={`tt_mc_cnr_${category.id}`}
                      size="2x"
                      color="success"
                    />
                  </Tooltipped>
                </Link>
              )} */}
            </div>
          </div>
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
  me,
  budgetPeriod,
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
            <div className="d-flex flex-wrap justify-content-between align-items-baseline">
              <div className="w-50">
                <h6 className="mb-0 mr-3 d-inline-block">
                  <Caret spaced />
                  {category.name} <span>({requestCount})</span>
                </h6>
              </div>
              <div className="mr-auto">
                {!!category.total_price_cents && (
                  <Tooltipped text={t('dashboard.subcat_total_sum')}>
                    <small className="mr-3" id={`ordqsc_tt_${category.id}`}>
                      <Icon.ShoppingCart className="mr-1" />
                      <samp>{formatCurrency(category.total_price_cents)}</samp>
                    </small>
                  </Tooltipped>
                )}
              </div>
              <div className="ml-3 mt-2 mt-md-0">
                {/* TODO: decide if/how to show these links
                {canRequest && (
                  <Link to={newRequestLink({ budgetPeriod, category })}>
                    <Tooltipped text={t('dashboard.create_request_for_subcat')}>
                      <Icon.PlusCircle
                        id={`tt_sc_cnr_${category.id}`}
                        size="2x"
                        color="success"
                      />
                    </Tooltipped>
                  </Link>
                )} */}
              </div>
            </div>
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

const newRequestLink = ({
  budgetPeriod = {},
  mainCategory = {},
  category = {}
}) => ({
  pathname: '/requests/new',
  search:
    '?' +
    stringifyQuery({
      bp: f.dehyphenUUID(budgetPeriod.id),
      mc: f.dehyphenUUID(mainCategory.id),
      c: f.dehyphenUUID(category.id)
    })
})
