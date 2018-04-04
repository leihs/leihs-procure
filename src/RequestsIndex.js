import React from 'react'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import * as Fragments from './GraphQlFragments'
import { RequestTotalAmount as TotalAmount } from './AppUtils'
import { Div, Row, Col, Badge } from './Bootstrap'
import Icon from './Icons'
import { MainWithSidebar } from './Layout'

const Loading = () => (
  <div className="w-100 p-3 h1 text-center">
    <Icon.Spinner /> Loading...
  </div>
)

const RequestLine = ({ fields }) => (
  <React.Fragment>
    <Row>
      <div className="col-sm-2">{fields.article_name}</div>
      <div className="col-sm-2">{fields.receiver_name}</div>
      <div className="col-sm-2">
        <code>Org #{fields.organization_id.split('-')[0]}</code>
      </div>
      <Col sm="1">
        <div className="badge badge-secondary">
          <Icon.PriceTag className="mr-1" />
          {fields.price_cents / 100} {fields.price_currency}
        </div>
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
        <div className="label label-default">{fields.priority}</div>
      </Col>
      <Col sm="1">
        <div className="label label-info">{fields.replacement}</div>
      </Col>
    </Row>
    {/* <pre>{JSON.stringify({ fields }, 0, 2)}</pre> */}
    <hr />
  </React.Fragment>
)

const FilterBar = () => (
  <Div cls="pt-2">
    <code>TODO</code>
  </Div>
)
const RequestsList = () => {
  const query = gql`
    {
      requests {
        ...RequestFieldsForIndex
      }
    }
    ${Fragments.RequestFieldsForIndex}
  `

  const render = ({ loading, error, data }) => {
    if (loading) return <Loading />
    if (error) return <p>Error :(</p>

    return data.requests.map(r => <RequestLine key={r.id} fields={r} />)
  }

  return <Query query={query}>{render}</Query>
}

const RequestsIndex = () => (
  <MainWithSidebar sidebar={<FilterBar />}>
    <RequestsList />
  </MainWithSidebar>
)

export default RequestsIndex
