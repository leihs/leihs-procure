import React from 'react'
import f from 'lodash'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import Loading from './Loading'
import { ErrorPanel } from './Error'

import { RequestTotalAmount as TotalAmount } from './decorators'
import { Div, Row, Col, Badge } from './Bootstrap'
import Icon from './Icons'
import RequestForm from './RequestForm'
import * as fragments from '../queries/fragments'

const RequestLineClosed = ({ request, onClick }) => (
  <Row className="row py-3 mx-0 cursor-pointer" onClick={onClick}>
    <Col sm="2">{request.article_name.value}</Col>
    <Col sm="2">{request.receiver.value}</Col>
    <Col sm="2">
      <code>Org #{request.organization.value.id.split('-')[0]}</code>
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

const UPDATE_REQUEST_MUTATION = gql`
  mutation updateRequest($requestData: RequestInput) {
    request(input_data: $requestData) {
      ...RequestFieldsForShow
    }
  }
  ${fragments.RequestFieldsForShow}
`

const valueIfWritable = (fields, requestData, reqKey, fieldKey) => {
  fieldKey = fieldKey || reqKey

  if (!f.get(requestData, reqKey)) {
    // eslint-disable-next-line no-debugger
    debugger
  }
  if (f.get(requestData, reqKey).write) {
    return { [fieldKey]: f.get(fields, fieldKey) }
  }
}

const updateRequestFromFields = (mutate, request, fields) => {
  // const requestData = {
  //   approved_quantity: fields.approved_quantity,
  //   article_name: fields.article_name,
  //   article_number: fields.article_number,
  //   inspection_comment: fields.inspection_comment,
  //   motivation: fields.motivation,
  //   order_quantity: fields.order_quantity,
  //   receiver: fields.receiver,
  //   requested_quantity: fields.requested_quantity,
  //   // NOTE: ignore for now bc we dont query it!
  //   // replacement: null,
  //   // NOTE: *_id mappings
  //   // building_id: fields.building, -- no building, just room!
  //   room_id: fields.room,
  //   // FIXME: those should not be required, can't change with this mutation!
  //   budget_period_id: request.budget_period.id,
  //   category_id: request.category.id,
  //   organization_id: request.organization.id,
  //   user_id: window.CURRENT_USER_ID,
  //   // NOTE: this must be sent (to identify request) but still cant be changed!
  //   id: request.id
  // }
  //

  const requestData = {
    ...valueIfWritable(fields, request, 'approved_quantity'),
    ...valueIfWritable(fields, request, 'article_name'),
    ...valueIfWritable(fields, request, 'article_number'),
    ...valueIfWritable(fields, request, 'inspection_comment'),
    ...valueIfWritable(fields, request, 'motivation'),
    ...valueIfWritable(fields, request, 'order_quantity'),
    ...valueIfWritable(fields, request, 'receiver'),
    ...valueIfWritable(fields, request, 'requested_quantity'),

    // NOTE: ignore for now bc we dont query it!
    // replacement: null,
    // NOTE: *_id mappings
    // building_id: fields.building, -- no building, just room!
    // room_id: fields.room,
    ...valueIfWritable(fields, request, 'room', 'room_id'),
    // NOTE: this must be sent (to identify request) but still cant be changed!
    id: request.id
  }

  console.log({ request, fields, requestData })

  mutate({ variables: { requestData } })
}

class RequestLine extends React.Component {
  state = {
    open: false
  }
  render({ props: { request, editQuery }, state } = this) {
    return state.open ? (
      <Query query={editQuery} variables={{ id: [request.id] }}>
        {({ error, loading, data }) => {
          if (loading) return <Loading />
          if (error) return <ErrorPanel error={error} data={data} />
          return (
            <Mutation mutation={UPDATE_REQUEST_MUTATION}>
              {(mutate, mutReq) => {
                if (mutReq.loading) return <Loading />
                if (mutReq.error)
                  return <ErrorPanel error={mutReq.error} data={mutReq.data} />
                const request = data.requests[0]
                return (
                  <RequestForm
                    className="p-3"
                    request={request}
                    onClose={() => this.setState({ open: false })}
                    onSubmit={fields =>
                      updateRequestFromFields(mutate, request, fields)
                    }
                  />
                )
              }}
            </Mutation>
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
