import React, { Fragment as F } from 'react'
import cx from 'classnames'
// import f from 'lodash'

import t from '../locale/translate'
import { DisplayName, RequestTotalAmount, formatCurrency } from './decorators'
import { Div, Row, Col, Badge, Tooltipped } from './Bootstrap'
import Icon from './Icons'
import RequestEdit from '../containers/RequestEdit'

class RequestLine extends React.Component {
  state = {
    open: false
  }
  render({ props: { request, editQuery, ...props }, state: { open } } = this) {
    const isChanged = false // FIXME: detect form changed state
    const lineStyle = cx(
      { 'cursor-pointer': !open || !isChanged },
      open && [
        'border border-top-0 border-right-0 border-left-0',
        'border-style-dashed',
        'o-50'
      ]
    )

    return (
      <F>
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
              requestId={request.id}
              onClose={() => this.setState({ open: false })}
              doChangeRequestCategory={props.doChangeRequestCategory}
              doChangeBudgetPeriod={props.doChangeBudgetPeriod}
              doDeleteRequest={props.doDeleteRequest}
            />
          </F>
        )}
      </F>
    )
  }
}
export default RequestLine

const RequestLineClosed = ({ request, onClick, className }) => (
  <Row className={cx('py-3 mx-0', className)} onClick={onClick}>
    <Col sm="2">{request.article_name.value}</Col>
    <Col sm="3">
      {request.receiver.value} / {DisplayName(request.organization.value)}
    </Col>
    <Col sm="4">
      <Tooltipped text={t('request_form_field.price_cents')}>
        <Badge secondary id={`price_cents_tt_${request.id}`}>
          <Icon.PriceTag className="mr-1" />
          {formatCurrency(request.price_cents.value)}
        </Badge>
      </Tooltipped>

      <Tooltipped text={t('request_form_field.requested_quantity')}>
        <Badge info cls="mr-1" id={`reqq_tt_${request.id}`}>
          {request.requested_quantity.value || '--'} <Icon.QuestionMark />
        </Badge>
      </Tooltipped>
      <Tooltipped text={t('request_form_field.approved_quantity')}>
        <Badge info cls="mr-1" id={`appq_tt_${request.id}`}>
          {request.approved_quantity.value || '--'} <Icon.Checkmark />
        </Badge>
      </Tooltipped>
      <Tooltipped text={t('request_form_field.order_quantity')}>
        <Badge info cls="mr-1" id={`ordq_tt_${request.id}`}>
          {request.order_quantity.value || '--'} <Icon.ShoppingCart />
        </Badge>
      </Tooltipped>
    </Col>
    <Col sm="2">
      <Tooltipped text={t('request_form_field.price_total')}>
        <Badge id={`totalam_tt_${request.id}`}>
          <Icon.ShoppingCart className="mr-1" />
          {formatCurrency(RequestTotalAmount(request))}
        </Badge>
      </Tooltipped>
    </Col>
    <Col sm="1">
      <Tooltipped text={t('priority')}>
        <Badge secondary cls="mr-1" id={`prio_tt_${request.id}`}>
          {t(`priority_label_${request.priority.value}`)}
        </Badge>
      </Tooltipped>
    </Col>
    <Col sm="1">
      {/* FIXME: replacement.value */}
      <Div cls="label label-info">{request.replacement.value}</Div>
    </Col>
  </Row>
)
