import React, { Fragment as F } from 'react'
import f from 'lodash'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

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
  StatefulForm
  // ButtonDropdown,
  // DropdownToggle,
  // DropdownMenu,
  // DropdownItem
} from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import { DisplayName } from '../components/decorators'
import UserAutocomplete from '../components/UserAutocomplete'

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
  }

  # fragment OrgProps on Organization {
  #   id
  #   name
  #   shortname
  # }
`

const RequestNewPage = () => (
  <MainWithSidebar>
    <h1>Antrag erstellen</h1>

    {window.isDebug && (
      <pre>
        <mark>
          - select: budget period, default is current requesting phase or param<br />
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

        // FIXME: find out if requesting after start of inspection is allowed?
        //        if not filter by `inspection_start_date`
        const budPeriods = f
          .sortBy(data.budget_periods, 'end_date')
          .filter(bp => new Date(bp.end_date).getTime() > Date.now())
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

        return (
          <StatefulForm
            idPrefix={`request_new`}
            values={{ budget_period: budPeriods[0].value }}
          >
            {({ fields, setValue, getValue, formPropsFor, ...formHelpers }) => {
              const subCats = SubCats(fields)

              const requester = getValue('requester')

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
                    <FormGroup label="Haupt-Kategorie">
                      <Select
                        {...formPropsFor('_main_category')}
                        required
                        options={mainCats}
                      />
                    </FormGroup>

                    <FormGroup label="Kategorie">
                      <Select
                        {...formPropsFor('category')}
                        required
                        disabled={f.isEmpty(subCats)}
                        options={subCats}
                      />
                    </FormGroup>

                    {/* FIXME: only show if allowed! */}
                    <FormGroup label="Für anderen Benutzer">
                      <Row>
                        <Col sm>
                          <span className="form-control-plaintext">
                            {requester ? DisplayName(requester) : 'Nein'}
                          </span>
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
                    </FormGroup>
                  </form>

                  {!!window.isDebug && (
                    <pre>{JSON.stringify({ fields }, 0, 2)}</pre>
                  )}
                </F>
              )
            }}
          </StatefulForm>
        )
      }}
    </Query>
  </MainWithSidebar>
)

export default RequestNewPage
