import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'
import { Link } from 'react-router-dom'
import { stringify as stringifyQuery } from 'qs'

import t from '../locale/translate'
import {
  Row,
  Col,
  Button,
  ButtonToolbar,
  // UncontrolledDropdown as Dropdown,
  // DropdownToggle,
  // DropdownMenu,
  // DropdownItem,
  Collapsing,
  Tooltipped
} from './Bootstrap'
import { formatCurrency, budgetPeriodDates } from './decorators'
import { MainWithSidebar } from './Layout'
import Icon from './Icons'
import Loading from './Loading'
import { ErrorPanel } from './Error'
import RequestLine from './RequestLine'
import ImageThumbnail from './ImageThumbnail'

import CurrentUser from '../containers/CurrentUserProvider'
import FilterBar from './RequestsFilterBar'
import logger from 'debug'
const log = logger('app:ui:RequestsDashboard')

class RequestsDashboard extends React.Component {
  render({ props } = this) {
    log('render', { props })
    const { requestsQuery, refetchAllData } = props

    const requestTotalCount =
      f.get(requestsQuery, 'data.dashboard.total_count') || 0

    const pageHeader = (
      <Row cls="pt-1">
        <Col sm>
          <h4>
            {requestsQuery.loading || !requestsQuery.data
              ? ' '
              : `${requestTotalCount} ${
                  requestTotalCount === 1
                    ? t('dashboard.requests_title_singular')
                    : t('dashboard.requests_title_plural')
                }`}
          </h4>
        </Col>
        <Col sm cls="d-flex justify-content-end align-items-end">
          <ButtonToolbar size="sm" className="mb-2">
            {/* TODO: export menu
            <Dropdown size="sm" small className="mr-1 mb-1">
              <DropdownToggle caret outline size="sm">
                <Icon.FileDownload /> {'Export'}
              </DropdownToggle>
              <DropdownMenu right>
                <DropdownItem>Dropdown Link</DropdownItem>
                <DropdownItem>Dropdown Link</DropdownItem>
              </DropdownMenu>
            </Dropdown> */}
            <Button
              size="sm"
              cls="mr-1 mb-1"
              outline
              title="refresh data"
              onClick={refetchAllData}
            >
              <Icon.Reload spin={requestsQuery.loading} />
            </Button>
          </ButtonToolbar>
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

  return f.get(data, 'dashboard.budget_periods').map(b => (
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
                          compactEditForm
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
            <Row>
              <Col sm="8">
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
              </Col>

              <Col
                sm="3"
                className="col-10 align-self-center order-last order-sm-12 text-right"
              >
                {!!budgetPeriod.total_price_cents && (
                  <Tooltipped text={t('dashboard.bp_total_sum')}>
                    <span
                      className="mr-2 f6"
                      id={`ordqb_tt_${budgetPeriod.id}`}
                    >
                      <samp>
                        {formatCurrency(budgetPeriod.total_price_cents)}{' '}
                      </samp>
                      <Icon.ShoppingCart />
                    </span>
                  </Tooltipped>
                )}
              </Col>

              <Col
                sm="1"
                className="col-2 mt-2 mt-sm-0 order-sm-last text-right"
              >
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
              </Col>
            </Row>
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
          <Row>
            <Col sm="8">
              <h5 className="mb-0 mr-sm-3 d-inline-block">
                <Caret spaced />
                <ImageThumbnail imageUrl={category.image_url} />
                {category.name} <small>({requestCount})</small>
              </h5>
            </Col>

            <Col
              sm="3"
              className="col-10 align-self-center order-last order-sm-12 text-right"
            >
              {!!category.total_price_cents && (
                <Tooltipped text={t('dashboard.maincat_total_sum')}>
                  <small className="mr-2 f6" id={`ordqmc_tt_${category.id}`}>
                    <samp>{formatCurrency(category.total_price_cents)} </samp>
                    <Icon.ShoppingCart />
                  </small>
                </Tooltipped>
              )}
            </Col>

            <Col sm="1" className="col-2 mt-2 mt-sm-0 order-sm-last text-right">
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
            </Col>
          </Row>
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
            <Row>
              <Col sm="8">
                <h6 className="mb-0 mr-sm-3 d-inline-block">
                  <Caret spaced />
                  {category.name} <span>({requestCount})</span>
                </h6>
              </Col>
              <Col
                sm="3"
                className="col-10 align-self-center order-last order-sm-12 text-right"
              >
                {!!category.total_price_cents && (
                  <Tooltipped text={t('dashboard.subcat_total_sum')}>
                    <small className="mr-2 f6" id={`ordqsc_tt_${category.id}`}>
                      <samp>{formatCurrency(category.total_price_cents)} </samp>
                      <Icon.ShoppingCart />
                    </small>
                  </Tooltipped>
                )}
              </Col>
              <Col
                sm="1"
                className="col-2 mt-2 mt-sm-0 order-sm-last text-right"
              >
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
              </Col>
            </Row>
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
