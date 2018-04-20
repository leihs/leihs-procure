import React from 'react'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import ControlledForm from '../components/ControlledForm'
import Icon from '../components/Icons'
import { Row, Col } from '../components/Bootstrap'

const mutationErrorHandler = err => {
  // not much we can do on backend error
  alert('Error! ' + err)
  window.location.reload()
}

const UPDATE_ADMINS_MUTATION = gql`
  mutation updateAdmins($adminUserList: [AdminInput]) {
    admins(input_data: $adminUserList) {
      id
      firstname
      lastname
      blubs
    }
  }
`

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

const AdminUsers = ({ data: { admins }, updateData, updatingInfo }) => (
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
                  updateData(fields)
                  // alert(JSON.stringify(fields, 0, 2))
                }}
              >
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

                <button
                  type="submit"
                  className="btn m-1 btn-primary"
                  disabled={updatingInfo.loading}
                >
                  <Icon.Checkmark /> <span>Speichern</span>
                </button>

                {/* <pre>{JSON.stringify(updatingInfo, 0, 2)}</pre> */}
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

const AdminUsersPage = () => (
  <Query query={USERS_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error)
        return (
          <p>
            Error :( <code>{error}</code>
          </p>
        )

      return (
        <Mutation
          mutation={UPDATE_ADMINS_MUTATION}
          onError={mutationErrorHandler}
          update={(cache, { data: { admins } }) => {
            // update the internal cache with the new data we received:
            cache.writeQuery({
              query: USERS_QUERY,
              data: { admins }
            })
          }}
        >
          {(updateAdmins, updatingInfo) => (
            <div>
              <AdminUsers
                data={data}
                updatingInfo={updatingInfo}
                updateData={formData =>
                  updateAdmins({
                    variables: {
                      adminUserList: formData.admins.map(id => ({
                        user_id: id
                      }))
                    }
                  })
                }
              />
            </div>
          )}
        </Mutation>
      )
    }}
  </Query>
)

export default AdminUsersPage
