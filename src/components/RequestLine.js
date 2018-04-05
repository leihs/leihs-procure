import React, { Fragment as F } from 'react'
// import f from 'lodash'

import { RequestTotalAmount as TotalAmount } from './decorators'
import { Div, Row, Col, Badge } from './Bootstrap'
import Icon from './Icons'

const RequestLine = ({ fields }) => (
  <F>
    <Row>
      <Col sm="2">{fields.article_name}</Col>
      <Col sm="2">{fields.receiver_name}</Col>
      <Col sm="2">
        <code>Org #{fields.organization_id.split('-')[0]}</code>
      </Col>
      <Col sm="1">
        <Div cls="badge badge-secondary">
          <Icon.PriceTag className="mr-1" />
          {fields.price_cents / 100} {fields.price_currency}
        </Div>
      </Col>
      <Col sm="3">
        <Badge info cls="mr-1" data-toggle="tooltip" title="Menge beantragt">
          {fields.requested_quantity || '--'} <Icon.QuestionMark />
        </Badge>
        <Badge info cls="mr-1" data-toggle="tooltip" title="Menge bewilligt">
          {fields.approved_quantity || '--'} <Icon.Checkmark />
        </Badge>
        <Badge info cls="mr-1" data-toggle="tooltip" title="Bestellmenge">
          {fields.order_quantity || '--'} <Icon.ShoppingCart />
        </Badge>
      </Col>
      <Col sm="1">
        <Badge>
          <Icon.ShoppingCart /> {TotalAmount(fields)}
        </Badge>
      </Col>
      <Col sm="1">
        <Div cls="label label-default">{fields.priority}</Div>
      </Col>
      <Col sm="1">
        <Div cls="label label-info">{fields.replacement}</Div>
      </Col>
    </Row>
    {/* <pre>{JSON.stringify({ fields }, 0, 2)}</pre> */}
    <hr />
  </F>
)
export default RequestLine
