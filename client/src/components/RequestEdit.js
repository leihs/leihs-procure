import React from 'react'
import f from 'lodash'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import Loading from './Loading'
import { ErrorPanel } from './Error'

// import { RequestTotalAmount as TotalAmount } from './decorators'
// import { Div, Row, Col, Badge } from './Bootstrap'
// import Icon from './Icons'
import RequestForm from './RequestForm'
import * as Fragments from '../graphql-fragments'

const REQUEST_EDIT_QUERY = gql`
  query RequestForEdit($id: [ID!]!) {
    requests(id: $id) {
      ...RequestFieldsForShow
    }
    # for selecting a new category:
    main_categories {
      id
      name
      categories {
        id
        name
      }
    }
    # for selecting a budget period:
    budget_periods {
      id
      name
    }
  }
  ${Fragments.RequestFieldsForShow}
`

const UPDATE_REQUEST_MUTATION = gql`
  mutation updateRequest($requestData: RequestInput) {
    request(input_data: $requestData) {
      ...RequestFieldsForShow
    }
  }
  ${Fragments.RequestFieldsForShow}
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

const boolify = (key, val) =>
  !val ? false : !val[key] ? null : val[key] === key

const updateRequestFromFields = (mutate, request, fields) => {
  const requestData = {
    ...valueIfWritable(fields, request, 'article_name'),
    ...valueIfWritable(fields, request, 'receiver'),
    ...valueIfWritable(fields, request, 'price_cents'),

    ...valueIfWritable(fields, request, 'requested_quantity'),
    ...valueIfWritable(fields, request, 'approved_quantity'),
    ...valueIfWritable(fields, request, 'order_quantity'),

    replacement: boolify(
      'replacement',
      valueIfWritable(fields, request, 'replacement')
    ),

    attachments: f.map(
      valueIfWritable(fields, request, 'attachments').attachments,
      o => ({ ...f.pick(o, 'id', '__typename'), to_delete: !!o.toDelete })
    ),

    // TODO: form field with id (autocomplete)
    // ...valueIfWritable(fields, request, 'supplier'),

    ...valueIfWritable(fields, request, 'article_number'),
    ...valueIfWritable(fields, request, 'motivation'),
    ...valueIfWritable(fields, request, 'priority'),
    ...valueIfWritable(fields, request, 'inspector_priority'),

    ...valueIfWritable(fields, request, 'inspection_comment'),

    ...valueIfWritable(fields, request, 'accounting_type'),
    ...valueIfWritable(fields, request, 'internal_order_number'),

    // NOTE: no building, just room!
    ...valueIfWritable(fields, request, 'room', 'room_id'),

    // NOTE: this must be sent (to identify request) but still cant be changed!
    id: request.id
  }

  mutate({ variables: { requestData } })
}

class RequestEdit extends React.Component {
  state = { selectNewCategory: false, selectBudgetPeriod: false }

  onSelectNewRequestCategory = () => {
    this.setState(s => ({
      selectNewCategory: !s.selectNewCategory
    }))
  }
  onChangeRequestCategory = newCategory => {
    const requestId = this.props.requestId
    if (!requestId || !newCategory.id) throw new Error()
    window.confirm(`Move to category "${newCategory.name}"?`) &&
      this.props.doChangeRequestCategory(requestId, newCategory.id)
  }

  onSelectNewBudgetPeriod = () => {
    this.setState(s => ({
      selectBudgetPeriod: !s.selectBudgetPeriod
    }))
  }
  onChangeBudgetPeriod = newBudgetPeriod => {
    const requestId = this.props.requestId
    if (!requestId || !newBudgetPeriod.id) throw new Error()
    window.confirm(`Move to Budget Period "${newBudgetPeriod.name}"?`) &&
      this.props.doChangeBudgetPeriod(requestId, newBudgetPeriod.id)
  }

  render({ requestId, onClose, ...props } = this.props) {
    return (
      <Query
        fetchPolicy="network-only"
        query={REQUEST_EDIT_QUERY}
        variables={{ id: [requestId] }}
      >
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
                    categories={data.main_categories}
                    budgetPeriods={data.budget_periods}
                    onClose={onClose}
                    onSubmit={fields =>
                      updateRequestFromFields(mutate, request, fields)
                    }
                    // action delete
                    doDeleteRequest={e => props.doDeleteRequest(request)}
                    // action move category
                    onSelectNewRequestCategory={this.onSelectNewRequestCategory}
                    isSelectingNewCategory={this.state.selectNewCategory}
                    doChangeRequestCategory={this.onChangeRequestCategory}
                    // action move budget period
                    onSelectNewBudgetPeriod={this.onSelectNewBudgetPeriod}
                    isSelectingNewBudgetPeriod={this.state.selectBudgetPeriod}
                    doChangeBudgetPeriod={this.onChangeBudgetPeriod}
                  />
                )
              }}
            </Mutation>
          )
        }}
      </Query>
    )
  }
}

export default RequestEdit
