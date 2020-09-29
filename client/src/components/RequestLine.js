import React, { Fragment as F } from 'react'
import cx from 'classnames'
// import f from 'lodash'

import t from '../locale/translate'
import { DisplayName, IdentifierDecorator, formatCurrency } from './decorators'
import { Row, Col, Badge, Tooltipped } from './Bootstrap'
import Icon from './Icons'
import RequestStateBadge from './RequestStateBadge'
import RequestEdit from '../containers/RequestEdit'

class RequestLine extends React.Component {
  state = {
    open: false
  }
  render(
    {
      props: { request, refetchQuery, compactEditForm, ...props },
      state: { open }
    } = this
  ) {
    const closeLine = () => this.setState({ open: false })
    const isChanged = false // FIXME: detect form changed state
    const wrapperStyle = cx(
      open ? 'border-top border-bottom' : 'bg-primary-shy-hover'
    )
    const lineStyle = cx(
      !open
        ? 'cursor-pointer'
        : [
            'border border-top-0 border-right-0 border-left-0',
            'border-style-dashed',
            // 'o-50'
            'bg-primary-shy'
          ]
    )

    return (
      <F>
        <div className={wrapperStyle}>
          <RequestLineClosed
            request={request}
            className={lineStyle}
            onClick={
              isChanged
                ? null
                : e => {
                    e.preventDefault()
                    this.setState({ open: !open })
                  }
            }
          />
          {!!open && (
            <F>
              <RequestEdit
                className="p-3"
                compactView={compactEditForm}
                requestId={request.id}
                refetchQuery={refetchQuery}
                onCancel={closeLine}
                onSuccess={closeLine}
                doChangeRequestCategory={props.doChangeRequestCategory}
                doChangeBudgetPeriod={props.doChangeBudgetPeriod}
                doDeleteRequest={props.doDeleteRequest}
              />
            </F>
          )}
        </div>
      </F>
    )
  }
}
export default RequestLine

export const RequestLineClosed = ({ request, onClick, className }) => (
  <Row className={cx('py-3 mx-0', className)} onClick={onClick}>
    <Col md="1">
      <IdentifierDecorator
        id={request.short_id}
        title={t('request_short_id')}
      />
    </Col>

    <Col md="2">
      {request.article_name.value || DisplayName(request.model.value)}
    </Col>

    <Col md="2">
      {DisplayName(request.user.value)} /{' '}
      {DisplayName(request.organization.value)}
    </Col>

    <Col md="1" cls="align-self-center text-md-center">
      <Tooltipped text={t('request_form_field.state')}>
        <RequestStateBadge
          state={request.state}
          id={`reqst_tt_${request.id}`}
          className="mr-1 text-wrap"
        />
      </Tooltipped>
    </Col>

    <Col md="1" className="align-self-center text-md-center">
      <Tooltipped text={t('priority')}>
        <Badge dark cls="mr-1" id={`prio_tt_${request.id}`}>
          {t(`priority_label_${request.priority.value}`)}
        </Badge>
      </Tooltipped>
    </Col>

    <Col md="2" className="align-self-center text-md-center">
      <div className="d-inline-block">
        <Tooltipped text={t('request_form_field.price_cents')}>
          <Badge secondary cls="mr-1" id={`price_cents_tt_${request.id}`}>
            <Icon.PriceTag className="mr-1" />
            <samp>{formatCurrency(request.price_cents.value)}</samp>
          </Badge>
        </Tooltipped>
      </div>
      <div className="d-inline-block">
        <Tooltipped text={t('request_form_field.requested_quantity')}>
          <Badge info cls="mr-1" id={`reqq_tt_${request.id}`}>
            {request.requested_quantity.value || '--'} <Icon.QuestionMark />
          </Badge>
        </Tooltipped>
        {!!request.approved_quantity.read && (
          <Tooltipped text={t('request_form_field.approved_quantity')}>
            <Badge info cls="mr-1" id={`appq_tt_${request.id}`}>
              {request.approved_quantity.value || '--'} <Icon.Checkmark />
            </Badge>
          </Tooltipped>
        )}
        {!!request.order_quantity.read && (
          <Tooltipped text={t('request_form_field.order_quantity')}>
            <Badge info cls="mr-1" id={`ordq_tt_${request.id}`}>
              {request.order_quantity.value || '--'} <Icon.ShoppingCart />
            </Badge>
          </Tooltipped>
        )}
      </div>
    </Col>

    <Col md="2" className="align-self-center text-right">
      <Tooltipped text={t('request_form_field.price_total')}>
        <span className="mr-2 f6" id={`totalam_tt_${request.id}`}>
          <samp>{formatCurrency(request.total_price_cents)} </samp>
          <Icon.ShoppingCart />
        </span>
      </Tooltipped>
    </Col>

    <Col md="1" />
  </Row>
)
