import React from 'react'
// import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

// import ControlledForm from '../components/ControlledForm'
import Icon from '../components/Icons'
import { Row, Col, Button, FormField } from '../components/Bootstrap'
import UserAutocomplete from '../components/UserAutocomplete'

const mutationErrorHandler = err => {
  // not much we can do on backend error
  window.confirm('Error! ' + err, () => window.location.reload())
}

// # DATA
//
const UPDATE_ADMINS_MUTATION = gql`
  mutation updateAdmins($adminUserList: [AdminInput]) {
    admins(input_data: $adminUserList) {
      id
      firstname
      lastname
    }
  }
`

const ADMIN_USERS_QUERY = gql`
  query AdminUsersPage {
    admins {
      id
      firstname
      lastname
    }
    requesters_organizations {
      user {
        id
        firstname
        lastname
      }
      organization {
        id
        name
      }
      department {
        id
        name
      }
    }
  }
`

// # VIEW
//
const AdminUsers = ({
  data: { admins },
  doRemoveAdmin,
  doAddAdmin,
  updatingInfo
}) => (
  <div className="pt-2 pb-3">
    <Row>
      <Col lg="2" />
      <Col lg="10">
        <h2>Users</h2>
        <h5>Procurement Admins</h5>
        <Row>
          <Col sm="6">
            <FormField label="current admins">
              <ul className="list-group list-group-compact">
                {admins.map(({ id, firstname, lastname }) => (
                  <li
                    key={id}
                    className="list-group-item d-flex justify-content-between align-items-center"
                  >
                    <span>
                      <Icon.User spaced className="mr-1" /> {firstname}{' '}
                      {lastname}
                    </span>
                    <Button
                      title="remove as admin"
                      color="link"
                      outline
                      flat
                      size="sm"
                      disabled={updatingInfo.loading}
                      onClick={() => doRemoveAdmin({ id })}
                    >
                      <Icon.Cross />
                    </Button>
                  </li>
                ))}
              </ul>
            </FormField>
          </Col>
          <Col sm="6">
            <div className="pr-3">
              <FormField label="add new admin">
                <UserAutocomplete onSelect={id => doAddAdmin(id)} />
              </FormField>
            </div>
          </Col>
        </Row>
        <hr />
        [TODO Users]
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

// # PAGE
//
const AdminUsersPage = () => (
  <Query query={ADMIN_USERS_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error)
        return (
          <p>
            Error :( <code>{error.toString()}</code>
          </p>
        )

      return (
        <Mutation
          mutation={UPDATE_ADMINS_MUTATION}
          onError={mutationErrorHandler}
          update={(cache, { data: { admins } }) => {
            // update the internal cache with the new data we received.
            // manual because apollo can't know by itself that the
            // mutation returns the same list as our query.
            cache.writeQuery({ query: ADMIN_USERS_QUERY, data: { admins } })
          }}
        >
          {(updateAdmins, updatingInfo) => (
            <AdminUsers
              data={data}
              updatingInfo={updatingInfo}
              doRemoveAdmin={({ id }) => {
                updateAdmins({
                  variables: {
                    adminUserList: data.admins
                      .filter(u => id !== u.id)
                      .map(({ id }) => ({ user_id: id }))
                  }
                })
              }}
              doAddAdmin={id => {
                updateAdmins({
                  variables: {
                    adminUserList: data.admins
                      .concat([{ id }])
                      .map(({ id }) => ({ user_id: id }))
                  }
                })
              }}
            />
          )}
        </Mutation>
      )
    }}
  </Query>
)

export default AdminUsersPage
