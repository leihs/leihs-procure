import React from 'react'
import f from 'lodash'

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
  FormField,
  InputText
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

const ListOfAdmins = ({ admins, doRemoveAdmin, doAddAdmin, updatingInfo }) => (
  <Row cls="mt-2">
    <Col sm="6">
      <FormField label="current admins">
        <ul className="list-group list-group-compact">
          {admins.map(user => (
            <li
              key={user.id}
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
                onClick={() => doRemoveAdmin({ id: user.id })}
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
        <UserAutocomplete
          excludeIds={f.isEmpty(admins) ? null : admins.map(({ id }) => id)}
          onSelect={user => doAddAdmin(user.id)}
        />
      </FormField>
    </Col>
  </Row>
)

const ListOfRequestersAndOrgs = ({ requesters, id = 'requesters_orgs' }) => (
  <Div cls="mt-2">
    <Row form cls="d-none d-sm-flex">
      <Col>
        <b>Name</b>
      </Col>
      <Col>
        <b>Departement</b>
      </Col>
      <Col>
        <b>Organisation</b>
      </Col>
      <Col sm="1" />
    </Row>

    <ControlledForm
      idPrefn={id}
      values={requesters}
      render={({ fields, formPropsFor, getValue, setValue }) => {
        return (
          <form
            id={id}
            onSubmit={e => {
              e.preventDefault()
              alert(JSON.stringify(fields, 0, 2))
            }}
          >
            {f.toArray(fields).map(({ user }, n) => (
              <Row form key={n}>
                <Col sm cls="bg-light">
                  <FormField
                    readOnly
                    cls="bg-light"
                    value={DisplayName(user)}
                    label={'user'}
                    hideLabel
                  />
                </Col>
                <Col sm>
                  <FormField
                    value={getValue(`${n}.department.name`)}
                    onChange={e => {
                      const val = e.target.value
                      setValue(`${n}.department.name`, val)
                    }}
                    label={'department'}
                    hideLabel
                  />
                </Col>
                <Col sm>
                  <FormField label={'organization'} hideLabel>
                    <InputText
                      value={getValue(`${n}.organization.name`)}
                      onChange={e => {
                        setValue(`${n}.organization.name`, e.target.value)
                      }}
                    />
                  </FormField>
                </Col>
                <Col sm="1">
                  <FormGroup>
                    <div className="form-check mt-2">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        {...formPropsFor(`[${n}][_delete]`)}
                      />
                      <label
                        className="form-check-label"
                        htmlFor={formPropsFor(`[${n}][_delete]`).id}
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
                  <UserAutocomplete
                    onSelect={user =>
                      setValue(`[${fields.length}][user]`, user)
                    }
                  />
                </Col>
                <Col>
                  <FormField label={'department'} hideLabel />
                </Col>
                <Col>
                  <FormField label={'organization'} hideLabel />
                </Col>
                <Col sm="1">[ok]</Col>
              </Row>
            </FormField>
          </form>
        )
      }}
    />
  </Div>
)
