import React from 'react'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import t from '../locale/translate'
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
      id
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
    <Row form cls="d-none d-sm-flex pb-2">
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
      idPrefix={id}
      values={requesters}
      render={({ fields, formPropsFor, getValue, setValue }) => {
        return (
          <form
            id={id}
            onSubmit={e => {
              e.preventDefault()
              // eslint-disable-next-line no-console
              console.log(fields)
              alert('fields (also see console): ' + JSON.stringify(fields, 0, 2))
            }}
          >
            {f
              .toArray(fields)
              .map(
                (
                  { id, user, department, organization, toDelete = false },
                  n
                ) => (
                  <Row
                    form
                    key={id || n}
                    cls={{
                      'text-strike': toDelete,
                      // new lines should show form validation styles
                      'was-validated': !id
                    }}
                  >
                    <Col sm>
                      <FormGroup label={'user'} hideLabel>
                        {/* TODO: make and use autocomplete-style version of InlineSearch */}
                        <InputText
                          readOnly
                          required
                          cls={toDelete ? 'bg-danger' : 'bg-light'}
                          value={DisplayName(user)}
                        />
                      </FormGroup>
                    </Col>
                    <Col sm>
                      <FormGroup label={'department'} hideLabel>
                        <InputText
                          readOnly={toDelete}
                          required
                          cls={toDelete && 'bg-danger'}
                          value={department && department.name}
                          onChange={e => {
                            setValue(`${n}.department.name`, e.target.value)
                          }}
                        />
                      </FormGroup>
                    </Col>
                    <Col sm>
                      <FormGroup label={'organization'} hideLabel>
                        <InputText
                          readOnly={toDelete}
                          required
                          cls={toDelete && 'bg-danger'}
                          value={organization && organization.name}
                          onChange={e => {
                            setValue(`${n}.organization`, {
                              name: e.target.value
                            })
                          }}
                        />
                      </FormGroup>
                    </Col>
                    <Col sm="1">
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
                    </Col>
                  </Row>
                )
              )}

            <FormField label="add new requester" cls="mt-2">
              <Row form>
                <Col>
                  <UserAutocomplete
                    onSelect={user =>
                      // adds a line to the form
                      setValue(`${Object.keys(fields).length}.user`, user)
                    }
                  />
                </Col>
                <Col>{/* <FormField label={'department'} hideLabel /> */}</Col>
                <Col>
                  {/* <FormField label={'organization'} hideLabel /> */}
                </Col>
                <Col sm="1" />
              </Row>
            </FormField>

            <button type="submit" className="btn m-1 btn-primary">
              <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
            </button>
            {/* <button type="button" className="btn m-1 btn-outline-secondary">
              {t('form_btn_cancel')}
            </button> */}
          </form>
        )
      }}
    />
  </Div>
)
