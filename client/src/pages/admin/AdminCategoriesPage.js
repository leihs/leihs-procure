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
import { Routed } from '../../components/Router'
import { DisplayName } from '../../components/decorators'
import Loading from '../../components/Loading'
import { ErrorPanel } from '../../components/Error'
import UserAutocomplete from '../../components/UserAutocomplete'

const CATEGORIES_INDEX_QUERY = gql`
  query MainCategoriesIndex {
    # for TableOfContents
    main_categories {
      id
      name
      image_url
      categories {
        id
        name
      }
    }
  }
`

const MAINCAT_PROPS_FRAGMENT = gql`
  fragment MainCatProps on MainCategory {
    id
    name
    can_delete
    image_url
    # FIXME: should return all *possible* limits (if set or not!)
    budget_limits {
      id
      amount_cents
      amount_currency
      budget_period {
        id
        name
        end_date
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
        ...UserProps
      }
      viewers {
        ...UserProps
      }
    }
  }
  fragment UserProps on User {
    id
    login
    firstname
    lastname
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
    main_categories(input_data: $mainCategories) {
      ...MainCatProps
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
  doUpdate: (mutate, mainCats) => {
    const data = mainCats.map(mainCat => {
      return {
        ...f.pick(mainCat, ['id', 'name']),
        // TODO: image: …
        budget_limits: f.map(mainCat.budget_limits, l => ({
          amount_cents: l.amount_cents,
          budget_period_id: l.budget_period.id
        })),
        categories: f
          .filter(mainCat.categories, mc => !mc.toDelete)
          .map(sc => ({
            ...f.pick(sc, [
              'id',
              'name',
              'procurement_account',
              'general_ledger_account',
              'cost_center'
            ]),
            inspectors: f
              .filter(sc.inspectors, u => !u.toDelete)
              .map(i => i.id),
            viewers: f.filter(sc.viewers, u => !u.toDelete).map(i => i.id)
          }))
      }
    })

    mutate({
      variables: { mainCategories: data }
    })
  },
  successFlash: t('form_message_save_success')
}

// # PAGE
//
const AdminCategoriesPage = ({ match }) => (
  <Query query={CATEGORIES_INDEX_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <Loading />
      if (error) return <ErrorPanel error={error} data={data} />

      const categoriesToc = data.main_categories
      const sidebar = (
        <F>
          <SideNav categories={categoriesToc} baseUrl={match.url} />
          <hr className="d-xl-none" />
        </F>
      )

      return (
        <MainWithSidebar sidebar={sidebar}>
          <Switch>
            <Route
              exact
              path={`${match.url}/:mainCatId`}
              render={r => <CategoryPage {...r} />}
              foo="bar"
            />
            {/* fallback/index content: */}
            <F>
              {/* NOTE: dont show index for now, only a TOC */}
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
              {/*  show toc */}
              <TableOfContents
                withSubcats
                categories={categoriesToc}
                baseUrl={match.url}
              />

              {/* preload rest of content */}
              <Query query={CATEGORIES_QUERY}>{() => false}</Query>
            </F>
          </Switch>
        </MainWithSidebar>
      )
    }}
  </Query>
)

export default AdminCategoriesPage

// # VIEW PARTIALS
//

class CategoryPage extends React.Component {
  state = { formKey: 1 }
  render({ match } = this.props) {
    return (
      <Routed>
        {({ setFlash }) => (
          <Mutation
            {...updateCategories.mutation}
            onCompleted={() => {
              this.setState({ formKey: Date.now() })
              setFlash({ message: updateCategories.successFlash })
              window && window.scrollTo(0, 0)
            }}
          >
            {(mutate, info) => (
              <Query query={CATEGORIES_QUERY}>
                {({ loading, error, data }) => {
                  if (loading) return <Loading />
                  if (error) return <ErrorPanel error={error} data={data} />

                  const mainCatId = f.enhyphenUUID(match.params.mainCatId)
                  const mainCat = f.find(data.main_categories, {
                    id: mainCatId
                  })

                  return (
                    <CategoryCard
                      {...mainCat}
                      formKey={this.state.formKey}
                      onSubmit={mainCat =>
                        updateCategories.doUpdate(mutate, [
                          mainCat,
                          ...data.main_categories.filter(
                            mc => mc.id !== mainCat.id
                          )
                        ])
                      }
                    />
                  )
                }}
              </Query>
            )}
          </Mutation>
        )}
      </Routed>
    )
  }
}

const extendWhere = (id, list, fn) =>
  list.map(o => (o.id !== id ? o : { ...o, ...fn(o) }))

const setAsDeleted = (toDelete, id, list) =>
  extendWhere(id, list, () => ({ toDelete }))

const CategoryCard = ({ id, formKey, onSubmit, ...props }) => {
  const formProps = ['name', 'image_url', 'budget_limits', 'categories']
  const formValues = f.pick(props, formProps)

  return (
    <StatefulForm
      key={id + formKey}
      idPrefix="budgetPeriods"
      values={formValues}
    >
      {({ fields, formPropsFor, getValue, setValue }) => {
        const onAddSubCat = () => {
          setValue('categories', [...fields.categories, {}])
        }
        const onMarkSubCatForDeletion = ({ id, toDelete = false }) => {
          setValue('categories', setAsDeleted(!toDelete, id, fields.categories))
        }
        const addUser = (fieldKey, cat, user) => {
          setValue(
            'categories',
            extendWhere(cat.id, fields.categories, c => ({
              [fieldKey]: [...c[fieldKey], user]
            }))
          )
        }
        const removeUser = (fieldKey, cat, { id, toDelete = false }) => {
          setValue(
            'categories',
            extendWhere(cat.id, fields.categories, c => ({
              [fieldKey]: setAsDeleted(!toDelete, id, c[fieldKey])
            }))
          )
        }
        const onAddInspector = (c, u) => addUser('inspectors', c, u)
        const onRemoveInspector = (c, u) => removeUser('inspectors', c, u)
        const onAddViewer = (c, u) => addUser('viewers', c, u)
        const onRemoveViewer = (c, u) => removeUser('viewers', c, u)

        return (
          <F>
            <div className="card mb-3" id={`mc${id}`}>
              <div className="card-header">
                <h4 className="mb-0">{fields.name}</h4>
              </div>
              <div className="card-body">
                <form
                  onSubmit={e => {
                    e.preventDefault()
                    onSubmit({ id, ...fields })
                  }}
                >
                  <Row>
                    <Col sm>
                      <h6>{t('admin.categories.image')}</h6>
                      {fields.image_url && (
                        <div className="img-thumbnail-wrapper mb-3">
                          <img
                            className="img-thumbnail"
                            alt={`thumbnail for category ${fields.name}`}
                            src={fields.image_url}
                          />
                        </div>
                      )}
                      <code>TBD: upload new image</code>
                    </Col>
                    <Col sm>
                      <h6>{t('admin.categories.budget_limits')}</h6>

                      {fields.budget_limits.map((l, i) => (
                        <Row key={l.id}>
                          <Col sm="4">{l.budget_period.name}</Col>
                          <Col sm>
                            <Let
                              limitField={formPropsFor(
                                `budget_limits.${i}.amount_cents`
                              )}
                            >
                              {({ limitField }) => (
                                <InputText
                                  cls="form-control-sm"
                                  {...limitField}
                                  value={(limitField.value || 0) / 100}
                                  onChange={e =>
                                    setValue(
                                      limitField.name,
                                      f.try(() => e.target.value * 100)
                                    )
                                  }
                                />
                              )}
                            </Let>
                          </Col>
                          <Col sm="4">{l.amount_currency}</Col>
                        </Row>
                      ))}
                    </Col>
                  </Row>

                  <Row>
                    <Col>
                      <h5 className="mt-4 mb-3">
                        {t('admin.categories.subcats')}
                      </h5>
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
                            label="category name"
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
                          <Col lg>
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

                          <Col lg>
                            <ListOfUsers
                              keyName="inspectors"
                              users={cat.inspectors}
                              onAddUser={u => onAddInspector(cat, u)}
                              onRemoveUser={u => onRemoveInspector(cat, u)}
                            />
                          </Col>

                          <Col lg>
                            <ListOfUsers
                              keyName="viewers"
                              users={cat.viewers}
                              onAddUser={u => onAddViewer(cat, u)}
                              onRemoveUser={u => onRemoveViewer(cat, u)}
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
                    <Button color="primary" type="submit">
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

const SideNav = ({ categories, baseUrl, withSubcats = false }) => (
  <nav className="pt-3">
    <h5>
      <NavLink className="nav-link text-dark" to={baseUrl}>
        {t('admin.categories.main_categories')}
      </NavLink>
    </h5>
    <TableOfContents
      categories={categories}
      baseUrl={baseUrl}
      withSubcats={withSubcats}
    />
  </nav>
)

const TableOfContents = ({ categories, baseUrl, withSubcats = false }) => (
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

        {!!withSubcats && (
          <ul className="list-unstyled text-muted ml-3 pl-3">
            {c.categories.map(subcat => (
              <F key={subcat.id}>
                <li>{subcat.name}</li>
              </F>
            ))}
          </ul>
        )}
      </li>
    ))}
  </ul>
)

// see AdminUsersPage/ListOfAdmins
const ListOfUsers = ({ keyName, users, onAddUser, onRemoveUser }) => (
  <React.Fragment>
    <Div>
      <FormGroup label={t(`admin.categories.${keyName}`)}>
        {f.isEmpty(users) ? (
          t('admin.categories.user_list_empty')
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
                  title={t(`admin.categories.remove_from_${keyName}`)}
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
      <FormGroup label={t(`admin.categories.add_to_${keyName}`)}>
        <UserAutocomplete
          excludeIds={f.isEmpty(users) ? null : users.map(({ id }) => id)}
          onSelect={onAddUser}
        />
      </FormGroup>
    </Div>
  </React.Fragment>
)

const Let = ({ children, ...props }) => children(props)
