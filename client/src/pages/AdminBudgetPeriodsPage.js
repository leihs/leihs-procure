import React from 'react'
import cx from 'classnames'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import t from '../locale/translate'
import Icon from '../components/Icons'
import {
  Button,
  Badge,
  StatefulForm,
  FormField,
  FormGroup,
  InputDate,
  Tooltipped
} from '../components/Bootstrap'
import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import { MainWithSidebar } from '../components/Layout'
import { formatCurrency } from '../components/decorators'

const mutationErrorHandler = err => {
  // not much we can do on backend error
  // eslint-disable-next-line no-console
  console.error(err)
  window.confirm('Error! ' + err) && window.location.reload()
}

// # DATA & ACTIONS
//
const ADMIN_BUDGET_PERIODS_FRAGMENTS = {
  props: gql`
    fragment AdminBudgetPeriodProps on BudgetPeriod {
      id
      name
      inspection_start_date
      end_date
      can_delete
      total_price_cents_requested_quantities
      total_price_cents_approved_quantities
    }
  `
}

const ADMIN_BUDGET_PERIODS_PAGE_QUERY = gql`
  query AdminBudgetPeriods {
    budget_periods {
      ...AdminBudgetPeriodProps
    }
  }
  ${ADMIN_BUDGET_PERIODS_FRAGMENTS.props}
`

const ADMIN_UPDATE_BUDGET_PERIODS_MUTATION = gql`
  mutation updateBudgetPeriods($budgetPeriods: [BudgetPeriodInput]) {
    budget_periods(input_data: $budgetPeriods) {
      ...AdminBudgetPeriodProps
    }
  }
  ${ADMIN_BUDGET_PERIODS_FRAGMENTS.props}
`

const updateBudgetPeriods = {
  mutation: {
    mutation: ADMIN_UPDATE_BUDGET_PERIODS_MUTATION,
    onError: mutationErrorHandler,

    update: (cache, { data: { budget_periods, ...data } }) => {
      // update the internal cache with the new data we received.
      cache.writeQuery({
        query: ADMIN_BUDGET_PERIODS_PAGE_QUERY,
        data: { budget_periods }
      })
    }
  },
  doUpdate: (mutate, fields) => {
    const data = fields
      .filter(i => !i.toDelete && !f.isEmpty(i.name))
      .map(i => f.pick(i, ['id', 'name', 'inspection_start_date', 'end_date']))

    mutate({
      variables: { budgetPeriods: data }
    })
  }
}

// # PAGE
//
class AdminBudgetPeriodsPage extends React.Component {
  state = { formKey: Date.now() }
  render() {
    return (
      <Mutation
        {...updateBudgetPeriods.mutation}
        onCompleted={() => this.setState({ formKey: Date.now() })}
      >
        {(mutate, info) => (
          <Query query={ADMIN_BUDGET_PERIODS_PAGE_QUERY} errorPolicy="all">
            {({ loading, error, data }) => {
              if (loading) return <Loading />
              if (error) return <ErrorPanel error={error} data={data} />

              const budgetPeriods = f.sortBy(data.budget_periods, 'end_date')

              return (
                <MainWithSidebar>
                  <h1>Budgetperioden</h1>
                  <BudgetPeriodsTable
                    budgetPeriods={budgetPeriods}
                    updateAction={fields =>
                      updateBudgetPeriods.doUpdate(mutate, fields)
                    }
                    key={this.state.formKey}
                  />
                </MainWithSidebar>
              )
            }}
          </Query>
        )}
      </Mutation>
    )
  }
}

export default AdminBudgetPeriodsPage

// # VIEW PARTIALS
//

const BudgetPeriodsTable = ({ budgetPeriods, updateAction }) => {
  const formValues = budgetPeriods

  return (
    <StatefulForm idPrefix="budgetPeriods" values={formValues}>
      {({ fields, formPropsFor, getValue, setValue }) => {
        const onSave = () => updateAction(fields)
        const onAddRow = () => {
          setValue(fields.length, {})
        }
        return (
          <React.Fragment>
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Startdatum der Prüfphase</th>
                  <th>Enddatum der Budgetperiode</th>
                  <th>Total Anträge</th>
                </tr>
              </thead>
              <tbody>
                {fields.map(({ toDelete = false, ...bp }, n) => (
                  <tr
                    key={n}
                    className={cx([
                      'rounded',
                      {
                        'text-strike bg-danger-light': toDelete,
                        // new lines are marked and should show form validation styles
                        'was-validated bg-info-light': !bp.id
                      }
                    ])}
                  >
                    <td>
                      <FormField
                        required
                        label="Name"
                        placeholder="Name"
                        hideLabel
                        readOnly={toDelete}
                        {...formPropsFor(`${n}.name`)}
                      />
                    </td>
                    <td>
                      <FormGroup label="Startdatum der Prüfphase" hideLabel>
                        <InputDate
                          required
                          readOnly={toDelete}
                          inputProps={{
                            className: cx({ 'text-strike ': toDelete })
                          }}
                          {...formPropsFor(`${n}.inspection_start_date`)}
                        />
                      </FormGroup>
                    </td>
                    <td>
                      <FormGroup label="Enddatum" hideLabel>
                        <InputDate
                          required
                          readOnly={toDelete}
                          inputProps={{
                            className: cx({ 'text-strike ': toDelete })
                          }}
                          {...formPropsFor(`${n}.end_date`)}
                        />
                      </FormGroup>
                    </td>
                    <td>
                      {bp.total_price_cents_requested_quantities > 0 &&
                      bp.total_price_cents_approved_quantities > 0 ? (
                        <React.Fragment>
                          <Tooltipped
                            text={'Total aller Anträge mit Status "Neu"'}
                          >
                            <Badge info id={`badge_requested_${n}`}>
                              <Icon.ShoppingCart />{' '}
                              {formatCurrency(
                                bp.total_price_cents_requested_quantities
                              )}
                            </Badge>
                          </Tooltipped>{' '}
                          <Tooltipped text={'Total aller bewilligten Anträge'}>
                            <Badge success id={`badge_approved_${n}`}>
                              <Icon.ShoppingCart />{' '}
                              {formatCurrency(
                                bp.total_price_cents_approved_quantities
                              )}
                            </Badge>
                          </Tooltipped>
                        </React.Fragment>
                      ) : (
                        <FormGroup>
                          <div className="form-check mt-2">
                            <label className="form-check-label">
                              <input
                                className="form-check-input"
                                type="checkbox"
                                checked={toDelete}
                                onChange={e => {
                                  setValue(`${n}.toDelete`, !!e.target.checked)
                                }}
                              />
                              {'remove'}
                            </label>
                          </div>
                        </FormGroup>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan="5">
                    <Tooltipped text="Neue Budgetperiode erstellen">
                      <Button color="link" id="add_bp_btn" onClick={onAddRow}>
                        <Icon.PlusCircle color="success" size="2x" />
                      </Button>
                    </Tooltipped>{' '}
                    <Button color="primary" onClick={onSave}>
                      <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
                    </Button>
                  </td>
                </tr>
              </tfoot>
            </table>
            {/* <pre>{JSON.stringify(fields, 0, 2)}</pre> */}
          </React.Fragment>
        )
      }}
    </StatefulForm>
  )
}
