import React from 'react'
// import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

// import ControlledForm from '../components/ControlledForm'
import Icon from '../components/Icons'
import {
  Row,
  Div,
  Col,
  Button,
  FormGroup,
  FormField
} from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import { DisplayName } from '../components/decorators'
import ControlledForm from '../components/ControlledForm'
import UserAutocomplete from '../components/UserAutocomplete'

const mutationErrorHandler = err => {
  // not much we can do on backend error
  window.confirm('Error! ' + err)
  window.location.reload()
}

// # DATA
//
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

const UPDATE_ADMINS_MUTATION = gql`
  mutation updateAdmins($adminUserList: [AdminInput]) {
    admins(input_data: $adminUserList) {
      id
      firstname
      lastname
    }
  }
`

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

// # VIEW PARTIALS
//
const ListOfAdmins = ({ admins, doRemoveAdmin, doAddAdmin, updatingInfo }) => (
  <Row cls="mt-2">
    <Col sm="6">
      <FormField label="current admins">
        <ul className="list-group list-group-compact">
          {admins.map(({ id, ...user }) => (
            <li
              key={id}
              className="list-group-item d-flex justify-content-between align-items-center"
            >
              <span>
                <Icon.User spaced className="mr-1" /> {DisplayName(user)}
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
      <FormField label="add new admin">
        <UserAutocomplete onSelect={id => doAddAdmin(id)} />
      </FormField>
    </Col>
  </Row>
)

const ListOfRequestersAndOrgs = ({ requesters, id = 'requesters_orgs' }) => (
  <Div cls="mt-2">
    <Row form cls="pb-2">
      <Col>
        <b>Name</b>
      </Col>
      <Col>
        <b>Departement</b>
      </Col>
      <Col>
        <b>Organisation</b>
      </Col>
    </Row>

    <ControlledForm
      idPrefix={id}
      values={requesters}
      render={({ fields, formPropsFor }) => {
        return (
          <form
            id={id}
            className="XXXwas-validated"
            onSubmit={e => {
              e.preventDefault()
              alert(JSON.stringify(fields, 0, 2))
            }}
          >
            {requesters.map(({ user, department, organization }, ix) => (
              <Row form key={user.id + department.id + organization.id}>
                <Col>
                  <FormField
                    {...formPropsFor(`requester[${ix}][user]`)}
                    label={'user'}
                    hideLabel
                    readOnly={false}
                  />
                </Col>
                <Col>
                  <FormField
                    {...formPropsFor(`requester[${ix}][department]`)}
                    label={'department'}
                    hideLabel
                  />
                </Col>
                <Col>
                  <FormField
                    {...formPropsFor(`requester[${ix}][organization]`)}
                    label={'organization'}
                    hideLabel
                  />
                </Col>
                <Col>
                  <FormGroup>
                    <div className="form-check mt-2">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        {...formPropsFor(`requester[${ix}][_delete]`)}
                      />
                      <label
                        className="form-check-label"
                        htmlFor={formPropsFor(`requester[${ix}][_delete]`).id}
                      >
                        {'remove'}
                      </label>
                    </div>
                  </FormGroup>
                </Col>
              </Row>
            ))}

            <FormField label="add new requester" cls="mt-2">
              <Row form>
                <Col>
                  <UserAutocomplete onSelect={id => alert(id)} />
                </Col>
                <Col>
                  <FormField label={'department'} hideLabel />
                </Col>
                <Col>
                  <FormField label={'organization'} hideLabel />
                </Col>
              </Row>
            </FormField>
          </form>
        )
      }}
    />
  </Div>
)

const AdminUsers = ({ data, doRemoveAdmin, doAddAdmin, updatingInfo }) => (
  <MainWithSidebar>
    <h2>Users</h2>

    <h5 className="pt-4">Procurement Admins</h5>
    <ListOfAdmins
      admins={data.admins}
      doRemoveAdmin={doRemoveAdmin}
      doAddAdmin={doAddAdmin}
      updatingInfo={updatingInfo}
    />

    <h3 className="pt-4">Requesters</h3>
    <ListOfRequestersAndOrgs requesters={data.requesters_organizations} />
  </MainWithSidebar>
)
