import React from 'react'
// import f from 'lodash'
import { Query } from 'react-apollo'
import Loading from './Loading'
import { ErrorPanel } from './Error'

import { RequestTotalAmount as TotalAmount } from './decorators'
import { Div, Row, Col, Badge } from './Bootstrap'
import Icon from './Icons'
import RequestForm from './RequestForm'

const RequestLineClosed = ({ request, onClick }) => (
  <Row className="row py-3 mx-0 cursor-pointer" onClick={onClick}>
    <Col sm="2">{request.article_name.value}</Col>
    <Col sm="2">{request.receiver.value}</Col>
    <Col sm="2">
      <code>Org #{request.organization.id.split('-')[0]}</code>
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
        {request.requested_quantity.value || '--'} <Icon.QuestionMark />
      </Badge>
      <Badge info cls="mr-1" data-toggle="tooltip" title="Menge bewilligt">
        {request.approved_quantity.value || '--'} <Icon.Checkmark />
      </Badge>
      <Badge info cls="mr-1" data-toggle="tooltip" title="Bestellmenge">
        {request.order_quantity.value || '--'} <Icon.ShoppingCart />
      </Badge>
    </Col>
    <Col sm="1">
      <Badge>
        <Icon.ShoppingCart /> {TotalAmount(request)}
      </Badge>
    </Col>
    {/* FIXME: priority */}
    {/* <Col sm="1">
      <Div cls="label label-default">{request.priority.value}</Div>
    </Col> */}
    <Col sm="1">
      <Div cls="label label-info">{request.replacement.value}</Div>
    </Col>
  </Row>
)

class RequestLine extends React.Component {
  state = {
    open: false
  }
  render({ props: { request, editQuery }, state } = this) {
    return state.open ? (
      <Query query={editQuery} variables={{ id: [request.id] }}>
        {({ error, loading, data }) => {
          if (loading) return <Loading />
          if (error) return <ErrorPanel error={error} />
          return (
            <RequestForm
              className="p-3"
              data={data}
              onClose={() => this.setState({ open: false })}
            />
          )
        }}
      </Query>
    ) : (
      <RequestLineClosed
        request={request}
        onClick={e => {
          e.preventDefault()
          this.setState({ open: true })
        }}
      />
    )
  }
}
export default RequestLine
