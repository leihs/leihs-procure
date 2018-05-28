import React, { Fragment as F } from 'react'
// import f from 'lodash'

import { RequestTotalAmount as TotalAmount } from './decorators'
import { Div, Row, Col, Badge } from './Bootstrap'
import Icon from './Icons'

const RequestLine = ({ fields }) => (
  <F>
    <Row>
      <Col sm="2">{fields.article_name.value}</Col>
      <Col sm="2">{fields.receiver.value}</Col>
      <Col sm="2">
        <code>Org #{fields.organization.id.split('-')[0]}</code>
      </Col>
      <Col sm="1">
        <Div cls="badge badge-secondary">
          <Icon.PriceTag className="mr-1" />
          {/* FIXME: price api */}
          PRICE
          {/* {fields.price_cents.value / 100} {fields.price_currency.value} */}
        </Div>
      </Col>
      <Col sm="3">
        <Badge info cls="mr-1" data-toggle="tooltip" title="Menge beantragt">
          {fields.requested_quantity.value || '--'} <Icon.QuestionMark />
        </Badge>
        <Badge info cls="mr-1" data-toggle="tooltip" title="Menge bewilligt">
          {fields.approved_quantity.value || '--'} <Icon.Checkmark />
        </Badge>
        <Badge info cls="mr-1" data-toggle="tooltip" title="Bestellmenge">
          {fields.order_quantity.value || '--'} <Icon.ShoppingCart />
        </Badge>
      </Col>
      <Col sm="1">
        <Badge>
          <Icon.ShoppingCart /> {TotalAmount(fields)}
        </Badge>
      </Col>
      <Col sm="1">
        <Div cls="label label-default">{fields.priority.value}</Div>
      </Col>
      <Col sm="1">
        <Div cls="label label-info">{fields.replacement.value}</Div>
      </Col>
    </Row>
    {/* <pre>{JSON.stringify({ fields }, 0, 2)}</pre> */}
    <hr />
  </F>
)
export default RequestLine
