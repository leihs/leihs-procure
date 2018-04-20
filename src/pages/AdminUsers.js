import React, { Fragement as F } from 'react'
import f from 'lodash'
import ControlledForm from '../components/ControlledForm'
import Icon from '../components/Icons'
import { Row, Col, FormField } from '../components/Bootstrap'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

const AdminUsers = ({ data: { admins } }) => (
  <div className="pt-2 pb-3">
    <Row>
      <Col lg="2" />
      <Col lg="10">
        <h2>Benutzer</h2>

        <h5>Admins</h5>

        <ControlledForm
          idPrefix="admin_users"
          values={{
            admins: admins.map(({ id }) => id)
          }}
          onChange={e => e}
          render={({ fields, formPropsFor }) => {
            const adminProps = formPropsFor('admins')
            return (
              <form
                id="request_form"
                className="XXXwas-validated"
                onSubmit={e => {
                  e.preventDefault()
                  alert(JSON.stringify(fields, 0, 2))
                }}>
                {/* <FormField label={'Administratoren'}> */}
                <Row>
                  <Col sm="3">
                    <textarea
                      className="form-control"
                      rows={fields.admins.length + 1}
                      value={fields.admins.join('\n')}
                      onChange={({ target: { value } }) =>
                        adminProps.updateValue(value.split('\n'))
                      }
                    />
                  </Col>
                  <Col sm="3">
                    {adminProps.value.map(id => {
                      const u = f.find(admins, { id })
                      if (!u) return false
                      const val = `${u.firstname} ${u.lastname}`
                      return (
                        <div key={id}>
                          <input
                            className="form-control"
                            readOnly
                            value={val}
                          />
                        </div>
                      )
                    })}
                  </Col>
                </Row>
                <button type="submit" className="btn m-1 btn-primary">
                  <Icon.Checkmark /> <span>Speichern</span>
                </button>
              </form>
            )
          }}
        />
      </Col>
    </Row>

    {/* <h3>Antragsteller/innen</h3>
    {[{ name: 'Adrian Brazerol', dep: 'Services', org: 'PZ' }].map(
      ({ name, dep, org }, i) => (
        <div key={i}>
          <FormField label={'name'} value={name} />
          <FormField label={'dep'} value={dep} />
          <FormField label={'org'} value={org} />
        </div>
      )
    )} */}
  </div>
)

const USERS_QUERY = gql`
  query AdminUsersPage {
    admins {
      id
      firstname
      lastname
    }
    # requesters_organizations {
    #   user {
    #     id
    #     firstname
    #     lastname
    #   }
    #   organization {
    #     id
    #     name
    #   }
    #   department {
    #     id
    #     name
    #   }
    # }
  }
`

const AdminUsersPage = () => (
  <Query query={USERS_QUERY}>
    {({ loading, error, data }) => {
      return error ? (
        'Error! ' + error
      ) : loading ? (
        'Loading…'
      ) : (
        <AdminUsers data={data} />
      )
    }}
  </Query>
)

export default AdminUsersPage
