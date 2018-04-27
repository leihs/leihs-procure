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
  alert('Error! ' + err)
  window.location.reload()
}

const UPDATE_ADMINS_MUTATION = gql`
  mutation updateAdmins($adminUserList: [AdminInput]) {
    admins(input_data: $adminUserList) {
      id
      firstname
      lastname
    }
  }
`

const ADMIN_USERS_PAGE_QUERY = gql`
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

const AdminUsers = ({ data: { admins }, doRemoveAdmin, updatingInfo }) => (
  <div className="pt-2 pb-3">
    <Row>
      <Col lg="2" />
      <Col lg="10">
        <h2>Benutzer</h2>
        <h5>Admins</h5>
        <Row>
          <Col sm="6">
            <ul className="list-group list-group-compact">
              {admins.map(({ id, firstname, lastname }) => (
                <li
                  key={id}
                  className="list-group-item d-flex justify-content-between align-items-center"
                >
                  <span>
                    <Icon.User spaced className="mr-1" /> {firstname} {lastname}
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
          </Col>
          <Col sm="6">
            <div className="pr-3">
              <FormField label="add new">
                <UserAutocomplete
                  onChange={selectedItem => console.log(selectedItem)}
                />
              </FormField>
            </div>
          </Col>
        </Row>
        <hr />
        <pre>{JSON.stringify(updatingInfo, 0, 2)}</pre>
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

const AdminUsersPage = () => (
  <Query query={ADMIN_USERS_PAGE_QUERY}>
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
              query: ADMIN_USERS_PAGE_QUERY,
              data: { admins }
            })
          }}
        >
          {(updateAdmins, updatingInfo) => (
            <div>
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
              />
            </div>
          )}
        </Mutation>
      )
    }}
  </Query>
)

export default AdminUsersPage
