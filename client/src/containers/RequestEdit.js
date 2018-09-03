import React, { Fragment as F } from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'

// import { RequestTotalAmount as TotalAmount } from '../components/decorators'
import t from '../locale/translate'
import { Alert, RoutedStatus } from '../components/Bootstrap'
// import Icon from '../components/Icons'
import RequestForm from '../components/RequestForm'
import * as Fragments from '../graphql-fragments'

const REQUEST_EDIT_QUERY = gql`
  query RequestForEdit($id: [ID!]!) {
    requests(id: $id) {
      ...RequestFieldsForEdit
      actionPermissions {
        delete
        edit
        moveBudgetPeriod
        moveCategory
      }
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
  ${Fragments.RequestFieldsForEdit}
`

const UPDATE_REQUEST_MUTATION = gql`
  mutation updateRequest($requestData: RequestInput) {
    request(input_data: $requestData) {
      ...RequestFieldsForEdit
    }
  }
  ${Fragments.RequestFieldsForEdit}
`

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
    window.confirm(t('request.confirm_move_category')) &&
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
    window.confirm(t('request.confirm_move_budget_period')) &&
      this.props.doChangeBudgetPeriod(requestId, newBudgetPeriod.id)
  }

  render({ requestId, onCancel, onSuccess, className, ...props } = this.props) {
    if (!f.isUUID(requestId)) {
      return (
        <RoutedStatus code={400}>
          <Alert color="danger">
            The Request id <samp>{requestId}</samp> is not valid!
          </Alert>
        </RoutedStatus>
      )
    }

    return (
      <Query
        fetchPolicy="network-only"
        query={REQUEST_EDIT_QUERY}
        variables={{ id: [requestId] }}
      >
        {({ error, loading, data }) => {
          if (loading) return <Loading />
          if (error) return <ErrorPanel error={error} data={data} />

          const request = f.first(data.requests)
          const p = f.get(request, 'actionPermissions')

          if (!request) {
            return (
              <RoutedStatus code={404}>
                <Alert color="danger">
                  No Request with id <samp>{requestId}</samp> found!
                </Alert>
              </RoutedStatus>
            )
          }

          return (
            <Mutation mutation={UPDATE_REQUEST_MUTATION}>
              {(mutate, mutReq) => {
                if (mutReq.loading) return <Loading />
                if (mutReq.error)
                  return <ErrorPanel error={mutReq.error} data={mutReq.data} />
                return (
                  <F>
                    <RequestForm
                      className={className}
                      request={request}
                      categories={data.main_categories}
                      budgetPeriods={data.budget_periods}
                      onCancel={onCancel}
                      onSubmit={
                        p.edit &&
                        (async fields => {
                          await mutate({
                            variables: {
                              requestData: {
                                id: request.id,
                                ...requestDataFromFields(request, fields)
                              }
                            }
                          })
                          onSuccess()
                        })
                      }
                      // action delete
                      doDeleteRequest={
                        !!(p.delete && props.doDeleteRequest) &&
                        (e => props.doDeleteRequest(request))
                      }
                      // action move category
                      onSelectNewRequestCategory={
                        this.onSelectNewRequestCategory
                      }
                      isSelectingNewCategory={this.state.selectNewCategory}
                      doChangeRequestCategory={
                        !!(p.moveCategory && props.doChangeRequestCategory) &&
                        this.onChangeRequestCategory
                      }
                      // action move budget period
                      onSelectNewBudgetPeriod={this.onSelectNewBudgetPeriod}
                      isSelectingNewBudgetPeriod={this.state.selectBudgetPeriod}
                      doChangeBudgetPeriod={
                        !!(p.moveBudgetPeriod && props.doChangeBudgetPeriod) &&
                        this.onChangeBudgetPeriod
                      }
                    />
                    {window.isDebug && (
                      <pre>{JSON.stringify({ request }, 0, 2)}</pre>
                    )}
                  </F>
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

RequestEdit.propTypes = {
  doChangeBudgetPeriod: PropTypes.func,
  doChangeRequestCategory: PropTypes.func,
  doDeleteRequest: PropTypes.func,
  onCancel: PropTypes.func,
  onSuccess: PropTypes.func
}

RequestEdit.defaultProps = {
  onCancel: f.noop,
  onSuccess: f.noop
}

const valueIfWritable = (fields, requestData, key) => {
  const reqField = f.get(requestData, key)
  // eslint-disable-next-line no-debugger
  if (!reqField) debugger // should not happen, ignore in prod
  if (reqField.write) return f.get(fields, key)
}

const fieldIfWritable = (fields, requestData, key) => {
  const value = valueIfWritable(fields, requestData, key)
  if (value) return { [key]: value }
}

export const requestDataFromFields = (request, fields) => {
  const boolify = (val, name) => f.presence(val) && name === val

  const model = valueIfWritable(fields, request, 'model')
  const user = valueIfWritable(fields, request, 'user')
  const supplier = valueIfWritable(fields, request, 'supplier')
  const room = valueIfWritable(fields, request, 'room')

  const requestData = {
    ...fieldIfWritable(fields, request, 'article_number'),
    ...fieldIfWritable(fields, request, 'receiver'),
    ...fieldIfWritable(fields, request, 'price_cents'),

    ...fieldIfWritable(fields, request, 'requested_quantity'),
    ...fieldIfWritable(fields, request, 'approved_quantity'),
    ...fieldIfWritable(fields, request, 'order_quantity'),

    ...fieldIfWritable(fields, request, 'motivation'),
    ...fieldIfWritable(fields, request, 'priority'),
    ...fieldIfWritable(fields, request, 'inspector_priority'),
    ...fieldIfWritable(fields, request, 'inspection_comment'),

    ...fieldIfWritable(fields, request, 'accounting_type'),
    ...fieldIfWritable(fields, request, 'internal_order_number'),

    ...(user ? { user: user.id } : null),

    replacement: boolify(
      valueIfWritable(fields, request, 'replacement'),
      'replacement'
    ),

    attachments: f.map(valueIfWritable(fields, request, 'attachments'), o => ({
      ...f.pick(o, 'id', '__typename'),
      to_delete: !!o.toDelete
    })),

    ...(model
      ? { model: model.id }
      : fieldIfWritable(fields, request, 'article_name')),

    ...(!fields._supplier_as_text
      ? supplier
        ? { supplier: supplier.id }
        : null
      : fieldIfWritable(fields, request, 'supplier_name')),

    // NOTE: no building, just room!
    ...(!room ? null : { room })
  }

  return requestData
}
