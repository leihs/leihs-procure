import React, { Fragment as F } from 'react'
import f from 'lodash'
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
  <F>
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
  </F>
)

const FilterBar = ({ filters, onFilterChange }) => (
  <Div cls="pt-2">
    <h5>Filters</h5>
    <fieldset>
      <legend className="h6">Budgetperioden</legend>
      {f.sortBy(filters.available.budgetPeriods, 'name').map(({ id, name }) => (
        <F key={id}>
          <label>
            <input
              type="radio"
              name="budgetPeriods"
              value={id}
              onChange={() => onFilterChange({ budgetPeriods: [id] })}
            />
            {name}
          </label>
          <br />
        </F>
      ))}
    </fieldset>
  </Div>
)

const RequestsIndex = ({ requests, filters, onFilterChange }) => (
  <MainWithSidebar
    sidebar={<FilterBar filters={filters} onFilterChange={onFilterChange} />}>
    <h4>{requests.length} Requests</h4>
    {requests.map(r => <RequestLine key={r.id} fields={r} />)}
  </MainWithSidebar>
)

class RequestsIndexWithData extends React.Component {
  constructor() {
    super()
    this.state = {
      currentFilters: {
        budgetPeriods: ['2292d02b-44cc-4342-8c76-0cc29ff7a92b']
      }
    }
    this.onFilterChange = this.onFilterChange.bind(this)
  }
  onFilterChange(filters) {
    this.setState({
      currentFilters: { ...this.state.currentFilters, ...filters }
    })
  }
  render({ state } = this) {
    const filters = state.currentFilters
    // FIXME: use real variables, not String interpolation!
    const query = gql`
    query RequestsIndex($budgetPeriods: [ID]) {
      # for filterbar:
      budget_periods {
        id
        name
      }
      # main index:
      requests(budget_period_id: ${JSON.stringify(filters.budgetPeriods)}) {
        ...RequestFieldsForIndex
      }
    }
    ${Fragments.RequestFieldsForIndex}
  `

    const render = ({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <p>Error :(</p>

      return (
        <RequestsIndex
          requests={data.requests}
          filters={{ available: { budgetPeriods: data.budget_periods } }}
          onFilterChange={this.onFilterChange}
        />
      )
    }

    return <Query query={query}>{render}</Query>
  }
}

export default RequestsIndexWithData
