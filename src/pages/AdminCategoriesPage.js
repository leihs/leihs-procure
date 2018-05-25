import React from 'react'
// import cx from 'classnames'
import f from 'lodash'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

// import t from '../locale/translate'
// import * as fragments from '../queries/fragments'
import Icon from '../components/Icons'
import {
  Div,
  Row,
  Col,
  Button,
  FormGroup,
  InputText,
  FormField
} from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import { DisplayName } from '../components/decorators'
import { ErrorPanel } from '../components/Error'
// import ControlledForm from '../components/ControlledForm'
import UserAutocomplete from '../components/UserAutocomplete'

const CATEGORIES_QUERY = gql`
  query MainCategories {
    main_categories {
      id
      name
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
        cost_center
        general_ledger_account
        inspectors {
          id
          login
          firstname
          lastname
        }
        name
      }
    }
  }
`

// # PAGE
//
const AdminCategoriesPage = () => (
  <Query query={CATEGORIES_QUERY}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error) return <ErrorPanel error={error} />

      return <AdminCategories categories={data.main_categories} />
    }}
  </Query>
)

export default AdminCategoriesPage

// # VIEW PARTIALS
//
const AdminCategories = ({ categories }) => (
  <MainWithSidebar sidebar={<TableOfContents categories={categories} />}>
    {categories
      .filter(({ id }) => id) // FIXME: remove when backend fixed
      .map(c => <CategoryCard {...c} key={c.id} />)}
  </MainWithSidebar>
)

const CategoryCard = ({ id, name, image_url, budget_limits, categories }) => (
  <div className="card mb-3" id={`cat-${id}`}>
    <div className="card-header">
      <h4 className="mb-0">{name}</h4>
    </div>
    <div className="card-body">
      <Row>
        <Col sm>
          <h6>Image</h6>
          {image_url && (
            <div className="img-thumbnail-wrapper mb-3">
              <img
                className="img-thumbnail"
                alt={`thumbnail for category ${name}`}
                src={image_url}
              />
            </div>
          )}
        </Col>
        <Col sm>
          <h6>Budget-Limits</h6>

          {budget_limits.map(l => (
            <Row key={l.id}>
              <Col sm="4">{l.budget_period.name}</Col>
              <Col sm>
                <InputText cls="form-control-sm" value={l.amount_cents} />
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

      {categories.map(cat => (
        <React.Fragment key={cat.id}>
          <FormField
            className="font-weight-bold"
            label="name"
            name="sub_category_name"
            hideLabel={true}
            defaultValue={cat.name}
          />
          <Row>
            <Col sm>
              <FormField
                className="form-control-sm"
                name="sub_category_cost_center"
                label="cost center"
                defaultValue={cat.cost_center}
              />
              <FormField
                className="form-control-sm"
                name="sub_category_account"
                label="account"
                defaultValue={cat.general_ledger_account}
              />
            </Col>
            <Col sm>
              <ListOfUsers users={cat.inspectors} />
            </Col>
          </Row>
          <hr />
        </React.Fragment>
      ))}
    </div>
  </div>
)

const TableOfContents = ({ categories }) => (
  <nav>
    <h5 className="px-3 pt-3">Categories</h5>
    <ul className="nav flex-column">
      {categories
        .filter(({ id }) => id) // FIXME: remove when backend fixed
        .map(c => (
          <li key={c.id} className="nav-item">
            <a className="nav-link active" href={`#cat-${c.id}`}>
              {c.name}
            </a>
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
const ListOfUsers = ({ users }) => (
  <React.Fragment>
    <Div>
      <FormGroup label="inspectors">
        <ul className="list-group list-group-compact">
          {users.map(user => (
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
                disabled={false}
                onClick={() => alert({ id: user.id })}
              >
                <Icon.Cross />
              </Button>
            </li>
          ))}
        </ul>
      </FormGroup>
    </Div>
    <Div>
      <FormGroup label="add new inspector">
        <UserAutocomplete
          excludeIds={f.isEmpty(users) ? null : users.map(({ id }) => id)}
          onSelect={user => alert(user.id)}
        />
      </FormGroup>
    </Div>
  </React.Fragment>
)
