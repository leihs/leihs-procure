import React from 'react'
// import cx from 'classnames'
import f from 'lodash'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

// import t from '../locale/translate'
// import * as fragments from '../queries/fragments'
import Icon from '../components/Icons'
import {
  Badge,
  StatefulForm,
  FormField,
  FormGroup,
  InputDate
} from '../components/Bootstrap'
import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import { MainWithSidebar } from '../components/Layout'
// import { DisplayName } from '../components/decorators'

// # DATA
//
const ADMIN_BUDGET_PERIODS_PAGE_QUERY = gql`
  query AdminBudgetPeriods {
    budget_periods {
      id
      name
      inspection_start_date
      end_date
      can_delete
      total_price_cents_requested_quantities
      total_price_cents_approved_quantities
    }
  }
`

// # PAGE
//
const AdminOrgsPage = () => (
  <Query query={ADMIN_BUDGET_PERIODS_PAGE_QUERY} errorPolicy="all">
    {({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <ErrorPanel error={error} data={data} />

      const budgetPeriods = f.sortBy(data.budget_periods, 'end_date')

      return (
        <MainWithSidebar>
          <h1>Budgetperioden</h1>
          <BudgetPeriodsTable budgetPeriods={budgetPeriods} />
        </MainWithSidebar>
      )
    }}
  </Query>
)

export default AdminOrgsPage

// # VIEW PARTIALS
//

const BudgetPeriodsTable = ({ budgetPeriods }) => {
  const formValues = f.fromPairs(f.map(budgetPeriods, bp => [bp.id, bp]))
  return (
    <StatefulForm idPrefix="budgetPeriods" values={formValues}>
      {({ fields, formPropsFor, getValue }) => (
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
              {budgetPeriods.map(bp => (
                <tr className="info" key={bp.id}>
                  <td>
                    <FormField
                      required
                      label="Name"
                      placeholder="Name"
                      hideLabel
                      {...formPropsFor(`${bp.id}.name`)}
                    />
                  </td>
                  <td>
                    <FormGroup label="Startdatum der Prüfphase" hideLabel>
                      <InputDate
                        required
                        {...formPropsFor(`${bp.id}.inspection_start_date`)}
                      />
                    </FormGroup>
                  </td>
                  <td>
                    <FormGroup label="Enddatum" hideLabel>
                      <InputDate
                        required
                        {...formPropsFor(`${bp.id}.end_date`)}
                      />
                    </FormGroup>
                  </td>
                  <td>
                    {/* TODO: tooltip */}
                    <Badge
                      info
                      title="Total aller Anträge mit Status &quot;Neu&quot;"
                    >
                      <Icon.ShoppingCart />{' '}
                      {/* TODO: money format CHF 24'430 */}
                      {bp.total_price_cents_requested_quantities}
                    </Badge>{' '}
                    {/* TODO: tooltip */}
                    <Badge success title="Total aller bewilligten Anträge">
                      <Icon.ShoppingCart />{' '}
                      {/* TODO: money format CHF 24'430 */}
                      {bp.total_price_cents_approved_quantities}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
            {/* <tfoot>
              <tr>
                <td className="h3" colSpan="3">
                  <i
                    className="fa fa-plus-circle"
                    data-toggle="tooltip"
                    title="Neue Budgetperiode erstellen"
                  />
                </td>
                <td className="text-right">
                  <button className="btn btn-success" type="submit">
                    <i className="fa fa-check" />
                    Speichern
                  </button>
                </td>
              </tr>
              <tr>
                <td colSpan="5">
                  <div className="flash" />
                </td>
              </tr>
            </tfoot> */}
          </table>
          {/* <pre>{JSON.stringify(fields, 0, 2)}</pre> */}
        </React.Fragment>
      )}
    </StatefulForm>
  )
}
