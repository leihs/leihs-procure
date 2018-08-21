import React, { Fragment as F } from 'react'
import cx from 'classnames'
import { Route, Switch, NavLink } from 'react-router-dom'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'
import t from '../../locale/translate'
import { mutationErrorHandler } from '../../apollo-client'
import Icon from '../../components/Icons'
import {
  Div,
  Row,
  Col,
  Button,
  StatefulForm,
  FormGroup,
  InputText,
  FormField,
  Tooltipped
} from '../../components/Bootstrap'
import { MainWithSidebar } from '../../components/Layout'
import { DisplayName } from '../../components/decorators'
import Loading from '../../components/Loading'
import { ErrorPanel } from '../../components/Error'
import UserAutocomplete from '../../components/UserAutocomplete'

const CATEGORIES_INDEX_QUERY = gql`
  query MainCategoriesIndex {
    main_categories {
      id
      name
      image_url
    }
  }
`

const MAINCAT_PROPS_FRAGMENT = gql`
  fragment MainCatProps on MainCategory {
    id
    name
    can_delete
    image_url
    budget_limits {
      id
      amount_cents
      amount_currency
      budget_period {
        id
        name
      }
    }
    categories {
      id
      name
      can_delete
      cost_center
      general_ledger_account
      procurement_account
      inspectors {
        id
        login
        firstname
        lastname
      }
    }
  }
`

// TODO: get single MainCat by id!
// Currently not too bad, rendering is more expensive than fetching,
// also due to caching it only fetches once anyhow!
const CATEGORIES_QUERY = gql`
  query MainCategories {
    main_categories {
      ...MainCatProps
    }
  }
  ${MAINCAT_PROPS_FRAGMENT}
`

const UPDATE_CATEGORIES_MUTATION = gql`
  mutation updateCategoriesPeriods($mainCategories: [MainCategoryInput]) {
    main_categories(input_data: [MainCategoryInput]) {
      ...AdminBudgetPeriodProps
    }
  }
  ${MAINCAT_PROPS_FRAGMENT}
`

const updateCategories = {
  mutation: {
    mutation: UPDATE_CATEGORIES_MUTATION,
    onError: mutationErrorHandler,

    update: (cache, { data: { main_categories } }) => {
      // update the internal cache with the new data we received.
      cache.writeQuery({ query: CATEGORIES_QUERY, data: { main_categories } })
    }
  },
  doUpdate: (mutate, mainCat) => {
    // TODO: handle user list(s)!
    // .filter(i => !i.toDelete && !f.isEmpty(i.name))
    // .map(i => f.pick(i, ['id', 'name', 'inspection_start_date', 'end_date']))
    const data = {
      ...mainCat,
      categories: mainCat.categories.filter(mc => !mc.toDelete).map(sc => ({
        ...sc,
        inspectors: sc.inspectors.filter(u => !u.toDelete)
      }))
    }

    if (!window.confirm(JSON.stringify(data, 0, 2))) return

    mutate({
      variables: { mainCategories: data }
    })
  }
}

// # PAGE
//
const AdminCategoriesPage = ({ match }) => (
  <Query query={CATEGORIES_INDEX_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <ErrorPanel error={error} data={data} />

      const categoriesToc = f.sortBy(data.main_categories, 'name')

      return (
        <MainWithSidebar
          sidebar={
            <TableOfContents categories={categoriesToc} baseUrl={match.url} />
          }
        >
          <Switch>
            <Route
              exact
              path={`${match.url}/:mainCatId`}
              render={r => <CategoryPage {...r} allData={data} />}
              foo="bar"
            />
            {/* NOTE: dont show index for now */}
            {/* show "index" with all content if no mainCat selected */}
            {/* <Route exact path={match.url}>
              <Query query={CATEGORIES_QUERY}>
                {({ loading, error, data }) => {
                  if (loading) return <Loading />
                  if (error) return <ErrorPanel error={error} data={data} />

                  return data.main_categories.map(c => (
                    <CategoryCard key={c.id} {...c} />
                  ))
                }}
              </Query>
            </Route> */}
          </Switch>
        </MainWithSidebar>
      )
    }}
  </Query>
)

export default AdminCategoriesPage

// # VIEW PARTIALS
//

const CategoryPage = ({ match, allData }) => (
  <Mutation
    {...updateCategories.mutation}
    onCompleted={() => this.setState({ formKey: Date.now() })}
  >
    {(mutate, info) => (
      <Query query={CATEGORIES_QUERY}>
        {({ loading, error, data }) => {
          if (loading) return <Loading />
          if (error) return <ErrorPanel error={error} data={data} />

          const mainCatId = f.enhyphenUUID(match.params.mainCatId)
          const mainCat = f.find(data.main_categories, { id: mainCatId })

          return (
            <CategoryCard
              {...mainCat}
              doUpdate={cat =>
                updateCategories.doUpdate(mutate, [
                  ...cat,
                  allData.main_categories.filter(mc => mc.id !== cat.id)
                ])
              }
            />
          )
        }}
      </Query>
    )}
  </Mutation>
)

const CategoryCard = ({ id, ...props }) => {
  const formValues = {
    ...f.pick(props, 'name', 'image_url', 'budget_limits'),
    categories: f.sortBy(props.categories, 'name')
  }
  return (
    <StatefulForm key={id} idPrefix="budgetPeriods" values={formValues}>
      {({ fields, formPropsFor, getValue, setValue }) => {
        const onAddSubCat = () => {
          setValue('categories', [...fields.categories, {}])
        }
        const onMarkSubCatForDeletion = ({ id, toDelete = false }) => {
          setValue(
            'categories',
            fields.categories.map(
              c => (c.id !== id ? c : { ...c, toDelete: !toDelete })
            )
          )
        }
        const onAddInspector = (cat, user) => {
          setValue('inspectors', [...fields.inspectors, user])
        }
        const onRemoveInspector = (cat, { id, toDelete = false }) => {
          setValue(
            'categories',
            fields.categories.map(
              c =>
                c.id !== cat.id
                  ? c
                  : {
                      ...c,
                      inspectors: c.inspectors.map(
                        u => (u.id !== id ? u : { ...u, toDelete: !toDelete })
                      )
                    }
            )
          )
        }
        return (
          <F>
            <div className="card mb-3" id={`cat-${id}`}>
              <div className="card-header">
                <h4 className="mb-0">{fields.name}</h4>
              </div>
              <div className="card-body">
                <form>
                  <Row>
                    <Col sm>
                      <h6>Image</h6>
                      {fields.image_url && (
                        <div className="img-thumbnail-wrapper mb-3">
                          <img
                            className="img-thumbnail"
                            alt={`thumbnail for category ${fields.name}`}
                            src={fields.image_url}
                          />
                        </div>
                      )}
                    </Col>
                    <Col sm>
                      <h6>Budget-Limits</h6>

                      {fields.budget_limits.map((l, i) => (
                        <Row key={l.id}>
                          <Col sm="4">{l.budget_period.name}</Col>
                          <Col sm>
                            <InputText
                              cls="form-control-sm"
                              {...formPropsFor(
                                `budget_limits.${i}.amount_cents`
                              )}
                            />
                          </Col>
                          <Col sm="4">{l.amount_currency}</Col>
                        </Row>
                      ))}
                    </Col>
                  </Row>

                  <Row>
                    <Col>
                      <h5 style={{ display: 'inline-block' }}>Subkategorien</h5>
                    </Col>
                  </Row>

                  {fields.categories.map((cat, i) => (
                    <React.Fragment key={cat.id}>
                      <Row>
                        <Col sm>
                          <FormField
                            className={cx('font-weight-bold', {
                              'text-danger text-strike': cat.toDelete
                            })}
                            hideLabel={true}
                            {...formPropsFor(`categories.${i}.name`)}
                          />
                        </Col>
                        {cat.can_delete && (
                          <Col sm="1">
                            <Tooltipped
                              text={t('admin.categories.delete_subcat')}
                            >
                              <label id={`btn_del_${cat.id}`} className="pt-1">
                                <Icon.Trash
                                  size="lg"
                                  className={cx(
                                    cat.toDelete ? 'text-dark' : 'text-danger'
                                  )}
                                />
                                <input
                                  type="checkbox"
                                  className="sr-only"
                                  checked={!!cat.toDelete}
                                  onClick={e => onMarkSubCatForDeletion(cat)}
                                />
                              </label>
                            </Tooltipped>
                          </Col>
                        )}
                      </Row>
                      {!cat.toDelete && (
                        <Row>
                          <Col sm>
                            <FormField
                              className="form-control-sm"
                              {...formPropsFor(`categories.${i}.cost_center`)}
                              label={t(
                                'admin.categories.subcategories.cost_center'
                              )}
                            />
                            <FormField
                              className="form-control-sm"
                              {...formPropsFor(
                                `categories.${i}.general_ledger_account`
                              )}
                              label={t(
                                'admin.categories.subcategories.general_ledger_account'
                              )}
                            />
                            <FormField
                              className="form-control-sm"
                              {...formPropsFor(
                                `categories.${i}.procurement_account`
                              )}
                              label={t(
                                'admin.categories.subcategories.procurement_account'
                              )}
                            />
                          </Col>
                          <Col sm>
                            <ListOfUsers
                              users={cat.inspectors}
                              onAddUser={u => onAddInspector(cat, u)}
                              onRemoveUser={u => onRemoveInspector(cat, u)}
                            />
                          </Col>
                        </Row>
                      )}
                      <hr />
                    </React.Fragment>
                  ))}

                  <div>
                    <Tooltipped text={t('admin.categories.add_subcat')}>
                      <Button
                        color="link"
                        id="add_bp_btn"
                        onClick={onAddSubCat}
                      >
                        <Icon.PlusCircle color="success" size="2x" />
                      </Button>
                    </Tooltipped>{' '}
                    <Button color="primary">
                      <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
                    </Button>
                  </div>
                </form>
              </div>
            </div>
            {window.isDebug && <pre>{JSON.stringify(fields, 0, 2)}</pre>}
          </F>
        )
      }}
    </StatefulForm>
  )
}

const TableOfContents = ({ categories, baseUrl }) => (
  <nav className="pt-3">
    <h5>
      <NavLink className="nav-link text-dark" to={baseUrl}>
        Categories
      </NavLink>
    </h5>
    <ul className="nav flex-column">
      {categories.map(c => (
        <li key={c.id} className="nav-item">
          <NavLink
            className="nav-link"
            activeClassName="disabled text-dark"
            to={`${baseUrl}/${f.dehyphenUUID(c.id)}`}
          >
            {c.name}
          </NavLink>
          {/* NOTE: dont show subcats for now */}
          {/* <ul className="list-unstyled text-muted">
              {c.categories.map(subcat => (
                <li key={subcat.id}>{subcat.name}</li>
              ))}
            </ul> */}
        </li>
      ))}
    </ul>
  </nav>
)

// see AdminUsersPage/ListOfAdmins
const ListOfUsers = ({ users, onAddUser, onRemoveUser }) => (
  <React.Fragment>
    <Div>
      <FormGroup label={t('admin.categories.inspectors')}>
        {f.isEmpty(users) ? (
          t('admin.categories.list_no_inspectors')
        ) : (
          <ul className="list-group list-group-compact">
            {users.map((user, i) => (
              <li
                key={user.id || i}
                className={cx(
                  'list-group-item d-flex justify-content-between align-items-center',
                  { 'bg-danger-light': user.toDelete }
                )}
              >
                <span className={cx({ 'text-strike': user.toDelete })}>
                  <Icon.User spaced className="mr-1" /> {DisplayName(user)}
                </span>
                <Button
                  title="remove as admin"
                  color="link"
                  outline
                  size="sm"
                  disabled={false}
                  onClick={() => onRemoveUser(user)}
                >
                  <Icon.Cross />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </FormGroup>
    </Div>
    <Div>
      <FormGroup label="add new inspector">
        <UserAutocomplete
          excludeIds={f.isEmpty(users) ? null : users.map(({ id }) => id)}
          onSelect={onAddUser}
        />
      </FormGroup>
    </Div>
  </React.Fragment>
)
