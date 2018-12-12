import React, { Fragment as F } from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'

import { DisplayName } from '../components/decorators'
import t from '../locale/translate'
import { Row, Col, Alert, RoutedStatus } from '../components/Bootstrap'
import Icon from '../components/Icons'
import RequestForm from '../components/RequestForm'
import RequestStateBadge from '../components/RequestStateBadge'
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
    budget_periods(whereRequestsCanBeMovedTo: $id) {
      id
      name
    }
    # for inspection comment presets
    settings {
      inspection_comments
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

  render(
    {
      requestId,
      refetchQuery,
      onCancel,
      onSuccess,
      className,
      compactView,
      withHeader,
      ...props
    } = this.props
  ) {
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
            <Mutation
              mutation={UPDATE_REQUEST_MUTATION}
              awaitRefetchQueries
              refetchQueries={f.presence(refetchQuery) && [refetchQuery]}
            >
              {(mutate, mutReq) => {
                if (mutReq.loading) return <Loading />
                if (mutReq.error)
                  return <ErrorPanel error={mutReq.error} data={mutReq.data} />
                return (
                  <F>
                    {withHeader && <RequestHeader data={data} />}
                    <RequestForm
                      className={className}
                      compactView={compactView}
                      request={request}
                      categories={data.main_categories}
                      budgetPeriods={data.budget_periods}
                      inspectionComments={data.settings.inspection_comments}
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
  onSuccess: f.noop
}

// UI

const RequestHeader = ({ data }) => {
  const r = data.requests[0]
  const bp = r.budget_period.value
  const sc = r.category.value
  const mc = sc.main_category

  return (
    <F>
      <dl>
        <Row cls="mt-3">
          <Col lg>
            <Row>
              <Col sm>
                <dt>{t('budget_period')}: </dt>
                <dd>{bp.name}</dd>
              </Col>
              <Col sm>
                <dt>{t('category')}: </dt>
                <dd>
                  {mc.name} <Icon.CaretRight /> {sc.name}
                </dd>
              </Col>
            </Row>
          </Col>
          <Col lg>
            <Row>
              <Col sm>
                <dt>{t('request_form_field.user')}: </dt>{' '}
                <dd>
                  {DisplayName(r.user.value)} /{' '}
                  {DisplayName(r.organization.value)}
                </dd>
              </Col>
              <Col sm>
                <dt>{t('request_state')}: </dt>
                <dd>
                  <RequestStateBadge
                    state={r.state}
                    id={`reqst_tt_${r.id}`}
                    className="mr-1 text-wrap"
                  />
                </dd>
              </Col>
            </Row>
          </Col>
        </Row>
        <hr />
      </dl>
    </F>
  )
}

// helpers

// FIXME: `valueIfWritable` not needed anymore, `withOnlyWritable` is enough
const valueIfWritable = (fields, requestData, key) => {
  const reqField = f.get(requestData, key)
  // eslint-disable-next-line no-debugger
  if (!reqField) debugger // should not happen, ignore in prod
  if (reqField.write) return f.get(fields, key)
}

const getField = (fields, requestData, key, fallback = undefined) => {
  return { [key]: f.get(fields, key) }
}

const withOnlyWritable = (formdata, requestFields) => {
  return f.pick(
    formdata,
    f.filter(f.map(requestFields, (v, k) => f.get(v, 'write') === true && k))
  )
}

export const requestDataFromFields = (request, fields) => {
  const boolify = (val, name) => f.presence(val) && name === val

  const model = valueIfWritable(fields, request, 'model')
  const user = valueIfWritable(fields, request, 'user')
  const supplier = valueIfWritable(fields, request, 'supplier')
  const room = valueIfWritable(fields, request, 'room')
  const approvedQ = valueIfWritable(fields, request, 'approved_quantity')
  const orderQ = valueIfWritable(fields, request, 'order_quantity')

  const requestData = {
    ...getField(fields, request, 'article_number'),
    ...getField(fields, request, 'receiver'),
    ...getField(fields, request, 'price_cents'),

    ...getField(fields, request, 'requested_quantity'),
    approved_quantity: f.present(approvedQ) ? approvedQ : null,
    order_quantity: f.present(orderQ) ? orderQ : null,

    ...getField(fields, request, 'motivation'),
    ...getField(fields, request, 'priority'),
    ...getField(fields, request, 'inspector_priority'),
    ...getField(fields, request, 'inspection_comment'),

    ...getField(fields, request, 'accounting_type'),
    ...getField(fields, request, 'internal_order_number'),

    ...(user ? { user: user.id } : null),

    replacement: boolify(
      valueIfWritable(fields, request, 'replacement'),
      'replacement'
    ),

    attachments: f.map(valueIfWritable(fields, request, 'attachments'), o => ({
      ...f.pick(o, 'id', 'typename'),
      to_delete: !!o.toDelete
    })),

    ...(model
      ? { model: model.id }
      : getField(fields, request, 'article_name')),

    ...(!fields._supplier_as_text
      ? supplier
        ? { supplier: supplier.id }
        : null
      : getField(fields, request, 'supplier_name')),

    // NOTE: no building, just room!
    ...(!room ? null : { room })
  }

  return withOnlyWritable(requestData, request)
}
