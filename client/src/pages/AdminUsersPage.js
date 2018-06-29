import React from 'react'
import cx from 'classnames'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import t from '../locale/translate'
import * as fragments from '../queries/fragments'
import Icon from '../components/Icons'
import {
  Row,
  Div,
  Col,
  Button,
  FormGroup,
  InputText,
  StatefulForm
} from '../components/Bootstrap'
import Loading from '../components/Loading'
import { MainWithSidebar } from '../components/Layout'
import { DisplayName } from '../components/decorators'
import { ErrorPanel } from '../components/Error'
import UserAutocomplete from '../components/UserAutocomplete'

const mutationErrorHandler = err => {
  // not much we can do on backend error
  window.confirm('Error! ' + err)
  window.location.reload()
}

// # DATA
//
const ADMIN_USERS_PAGE_QUERY = gql`
  query AdminUsersPage {
    admins {
      id
      firstname
      lastname
    }
    requesters_organizations {
      ...RequesterOrg
    }
  }
  ${fragments.RequesterOrg}
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

const UPDATE_REQUESTERS_MUTATION = gql`
  mutation updateRequestersOrgs(
    $requestersOrgsList: [RequesterOrganizationInput]
  ) {
    requesters_organizations(input_data: $requestersOrgsList) {
      ...RequesterOrg
    }
  }
  ${fragments.RequesterOrg}
`

// # PAGE
//
const AdminUsersPage = () => (
  <Query query={ADMIN_USERS_PAGE_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <ErrorPanel error={error} data={data} />

      // actions for admins list
      const updateAdmins = {
        mutation: {
          mutation: UPDATE_ADMINS_MUTATION,
          onError: mutationErrorHandler,
          update: (cache, { data: { admins } }) => {
            // update the internal cache with the new data we received.
            // manual because apollo can't know by itself that the
            // mutation returns the same list as our query.
            cache.writeQuery({
              query: ADMIN_USERS_PAGE_QUERY,
              data: { ...data, admins }
            })
          }
        },
        doRemoveAdmin: (mutate, { id }) => {
          mutate({
            variables: {
              adminUserList: data.admins
                .filter(u => id !== u.id)
                .map(({ id }) => ({ user_id: id }))
            }
          })
        },
        doAddAdmin: (mutate, id) => {
          mutate({
            variables: {
              adminUserList: data.admins
                .concat([{ id }])
                .map(({ id }) => ({ user_id: id }))
            }
          })
        }
      }

      // actions for requester/orgs list
      const updateRequestersOrgs = {
        mutation: {
          mutation: UPDATE_REQUESTERS_MUTATION,
          onError: mutationErrorHandler,
          update: (cache, { data: { requesters_organizations } }) => {
            cache.writeQuery({
              query: ADMIN_USERS_PAGE_QUERY,
              data: { ...data, requesters_organizations }
            })
          }
        },
        doUpdate: (mutate, fields) => {
          const data = f
            .toArray(fields)
            .filter(f => !f.toDelete)
            .map(f => ({
              user_id: f.user.id,
              department: f.department.name,
              organization: f.organization.name
            }))
          mutate({
            variables: { requestersOrgsList: data }
          })
        }
      }

      return (
        <AdminUsers
          data={data}
          updateAdmins={updateAdmins}
          updateRequestersOrgs={updateRequestersOrgs}
        />
      )
    }}
  </Query>
)

export default AdminUsersPage

// # VIEW PARTIALS
//
const AdminUsers = ({ data, updateAdmins, updateRequestersOrgs }) => (
  <MainWithSidebar>
    <h2>Users</h2>

    <h5 className="pt-4">Procurement Admins</h5>
    <ListOfAdmins admins={data.admins} updateAdmins={updateAdmins} />

    <h3 className="pt-4">Requesters</h3>
    <ListOfRequestersAndOrgs
      requesters={data.requesters_organizations}
      updateRequestersOrgs={updateRequestersOrgs}
    />
  </MainWithSidebar>
)

const ListOfAdmins = ({ admins, updateAdmins }) => (
  <Mutation {...updateAdmins.mutation}>
    {(mutate, { loading }) => (
      <div className="form-shade-wrapper">
        <div className={cx('form-shade', { 'form-shade-blocked': loading })} />
        <Row cls="mt-2">
          <Col sm="6">
            <FormGroup label="current admins">
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
                      size="sm"
                      disabled={loading}
                      onClick={() =>
                        updateAdmins.doRemoveAdmin(mutate, { id: user.id })
                      }
                    >
                      <Icon.Cross />
                    </Button>
                  </li>
                ))}
              </ul>
            </FormGroup>
          </Col>
          <Col sm="6">
            <FormGroup label="add new admin">
              <UserAutocomplete
                excludeIds={
                  f.isEmpty(admins) ? null : admins.map(({ id }) => id)
                }
                onSelect={user => updateAdmins.doAddAdmin(mutate, user.id)}
              />
            </FormGroup>
          </Col>
        </Row>
      </div>
    )}
  </Mutation>
)

const ListOfRequestersAndOrgs = ({
  requesters,
  id = 'requesters_orgs',
  updateRequestersOrgs
}) => (
  <Mutation {...updateRequestersOrgs.mutation}>
    {(mutate, updatingInfo) => (
      <Div cls="mt-2 form-group-lines">
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
          <Col sm="2" />
        </Row>

        <StatefulForm idPrefix={id} values={requesters}>
          {({ fields, formPropsFor, getValue, setValue }) => (
            <form
              id={id}
              onSubmit={e => {
                e.preventDefault()
                updateRequestersOrgs.doUpdate(mutate, fields)
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
                      cls={[
                        'rounded',
                        {
                          'text-strike bg-danger-light': toDelete,
                          // new lines are marked and should show form validation styles
                          'was-validated bg-info-light': !id
                        }
                      ]}
                    >
                      <Col sm>
                        <FormGroup label={'user'} hideLabel>
                          {/* TODO: make and use autocomplete-style version of InlineSearch
                            - field will get 'invalid' styles if no user id present
                        */}
                          <InputText
                            readOnly
                            required
                            cls="bg-light"
                            value={DisplayName(user)}
                          />
                        </FormGroup>
                      </Col>
                      <Col sm>
                        <FormGroup label={'department'} hideLabel>
                          <InputText
                            readOnly={toDelete}
                            required
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
                            value={organization && organization.name}
                            onChange={e => {
                              setValue(`${n}.organization`, {
                                name: e.target.value
                              })
                            }}
                          />
                        </FormGroup>
                      </Col>
                      <Col sm="2">
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

              <FormGroup label="add new requester" cls="mt-2">
                <Row form>
                  <Col>
                    <UserAutocomplete
                      onSelect={user =>
                        // adds a line to the form
                        setValue(`${Object.keys(fields).length}.user`, user)
                      }
                    />
                  </Col>
                  <Col>
                    {/* <FormField label={'department'} hideLabel /> */}
                  </Col>
                  <Col>
                    {/* <FormField label={'organization'} hideLabel /> */}
                  </Col>
                  <Col sm="2" />
                </Row>
              </FormGroup>

              <button type="submit" className="btn m-1 btn-primary btn-massive">
                <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
              </button>
              {/* <button type="button" className="btn m-1 btn-outline-secondary btn-massive">
              {t('form_btn_cancel')}
            </button> */}
            </form>
          )}
        </StatefulForm>
      </Div>
    )}
  </Mutation>
)
