import React, { Fragment as F } from 'react'
import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'
// import qs from 'qs'

// import * as CONSTANTS from '../constants'
// import t from '../locale/translate'
// import Icon from '../components/Icons'
import {
  Row,
  Col,
  // FilePicker,
  FormGroup,
  // FormField,
  Select,
  // ButtonRadio,
  StatefulForm,
  // ButtonDropdown,
  // DropdownToggle,
  // DropdownMenu,
  // DropdownItem,
  RouteParams
} from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import { DisplayName } from '../components/decorators'
import UserAutocomplete from '../components/UserAutocomplete'

import RequestForm from '../components/RequestForm'

const NEW_REQUEST_QUERY = gql`
  query newRequestQuery {
    budget_periods {
      id
      name
      inspection_start_date
      end_date
    }
    main_categories {
      id
      name
      categories {
        id
        name
      }
    }
    templates {
      id
      category {
        id
      }
      article_name
      article_number
      model {
        id
        product
      }
      price_cents
      price_currency
      supplier {
        id
      }
    }
  }
`

// TODO: form-selection-to-params
// const updateQueryParams = ({ fields, params, location }) => {
//   const formParams = {
//     ...f.pick(fields, ['budget_period', 'category']),
//     requester: f.get(fields, 'requester.id')
//   }
//   return {
//     ...location,
//     search: '?' + qs.stringify({ ...params, ...formParams })
//   }
// }

class RequestNewPage extends React.Component {
  // TODO: form-selection-to-params
  //      - apply url params as state.initalSelection
  //      - needs query-user-by-id so we get the full object w/ name from uuid.
  // constructor() {
  //   this.state = {initalSelection: …}
  // }
  render() {
    return (
      <RouteParams>
        {({ params, location, history }) => (
          <MainWithSidebar>
            <h1>Antrag erstellen</h1>

            {window.isDebug && (
              <pre>
                <mark>
                  - select: budget period, default is current requesting phase
                  or param<br />
                  - select: category, default is param <br />
                  - select: template, default is param<br />
                  - select: user, default is param
                </mark>
              </pre>
            )}

            <Query query={NEW_REQUEST_QUERY} networkPolicy="cache-then-network">
              {({ loading, error, data }) => {
                if (loading) return <Loading />
                if (error) return <ErrorPanel error={error} data={data} />

                return (
                  <NewRequestForm
                    data={data}
                    // TODO: form-selection-to-params
                    // onSelectionChange={fields =>
                    //   history.replace(
                    //     updateQueryParams({ params, location, fields })
                    //   )
                    // }
                    onSelectionChange={() => {}}
                  />
                )
              }}
            </Query>
          </MainWithSidebar>
        )}
      </RouteParams>
    )
  }
}

export default RequestNewPage

const NewRequestForm = ({ data, onSelectionChange }) => {
  const budPeriods = f
    .sortBy(data.budget_periods, 'end_date')
    .filter(bp => new Date(bp.inspection_start_date).getTime() > Date.now())
    .map(bp => ({
      value: bp.id,
      label: `${bp.name} – Antragsphase bis ${new Date(
        bp.inspection_start_date
      ).toLocaleDateString()}`
    }))

  const mainCats = data.main_categories.map(mc => ({
    value: mc.id,
    label: mc.name
  }))

  const SubCats = fields =>
    !f.present(fields._main_category)
      ? []
      : f
          .find(data.main_categories, {
            id: fields._main_category
          })
          .categories.map(mc => ({
            value: mc.id,
            label: mc.name
          }))

  const Templates = fields =>
    f
      .filter(data.templates, {
        category: { id: fields.category }
      })
      .map(mc => ({
        value: mc.id,
        label: mc.article_name
      }))

  return (
    <StatefulForm
      idPrefix={`request_new`}
      values={{ budget_period: budPeriods[0].value }}
      onChange={fields => onSelectionChange(fields)}
    >
      {({ fields, setValue, getValue, formPropsFor, ...formHelpers }) => {
        const subCats = SubCats(fields)
        const requester = getValue('requester')
        const templates = Templates(fields)

        const showForm = f.present(fields.template)
        const selectedTemplate = f.find(data.templates, { id: fields.template })

        return (
          <F>
            <form
              onSubmit={e => {
                e.preventDefault()
                window.alert('TODO!')
              }}
            >
              <FormGroup label="Budgetperiode">
                <Select
                  {...formPropsFor('budget_period')}
                  required
                  emptyOption={false}
                  options={budPeriods}
                />
              </FormGroup>
              <Row>
                <Col lg>
                  <FormGroup label="Haupt-Kategorie">
                    <Select
                      {...formPropsFor('_main_category')}
                      required
                      options={mainCats}
                    />
                  </FormGroup>
                </Col>
                <Col lg>
                  <FormGroup label="Kategorie">
                    <Select
                      {...formPropsFor('category')}
                      required
                      disabled={f.isEmpty(subCats)}
                      options={subCats}
                    />
                  </FormGroup>
                </Col>
              </Row>

              {/* FIXME: only show if allowed! */}
              <FormGroup
                label={
                  <span>
                    Für anderen Benutzer{' '}
                    <code>
                      {'// TBD: permissions for creating for other user'}
                    </code>
                  </span>
                }
              >
                <Row>
                  <Col sm>
                    <b className="form-control-plaintext">
                      {requester ? DisplayName(requester) : 'Nein'}
                    </b>
                  </Col>
                  <Col sm>
                    <UserAutocomplete
                      inputProps={{
                        placeholder: 'Benutzer auswählen'
                      }}
                      onSelect={u => setValue('requester', u)}
                    />
                  </Col>
                </Row>

                <FormGroup label="Vorlage">
                  <Select
                    {...formPropsFor('template')}
                    required
                    disabled={f.isEmpty(subCats)}
                    options={templates}
                  />
                </FormGroup>
              </FormGroup>
            </form>

            {!!window.isDebug && (
              <pre>{JSON.stringify({ selectedTemplate, templates }, 0, 2)}</pre>
            )}

            {!!window.isDebug && <pre>{JSON.stringify({ fields }, 0, 2)}</pre>}

            {showForm && (
              <RequestForm
                className="pt-3"
                // NOTE: reset form if preselection changes
                key={['budget_period', 'category', 'template']
                  .map(k => [fields[k]])
                  .join()}
                request={selectedTemplate}
                categories={data.main_categories}
                budgetPeriods={data.budget_periods}
                onClose={e => window.alert('TODO')}
                onSubmit={fields => window.alert(JSON.stringify(fields, 0, 2))}
              />
            )}
          </F>
        )
      }}
    </StatefulForm>
  )
}
