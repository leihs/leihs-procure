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

const boolify = (key, val) => (!val[key] ? null : val[key] === key)

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

    // TODO: form field with id (autocomplete)
    // ...valueIfWritable(fields, request, 'supplier'),
    ...valueIfWritable(fields, request, 'supplier_name'),

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

const RequestEdit = ({ requestId, onClose, ...props }) => (
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
                onClose={onClose}
                onSubmit={fields =>
                  updateRequestFromFields(mutate, request, fields)
                }
                doDeleteRequest={e => props.doDeleteRequest(request)}
                doChangeRequestCategory={props.doChangeRequestCategory}
              />
            )
          }}
        </Mutation>
      )
    }}
  </Query>
)

export default RequestEdit
