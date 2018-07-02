import React, { Fragment as F } from 'react'
import { Route, Switch, Redirect, NavLink } from 'react-router-dom'
// import cx from 'classnames'
import f from 'lodash'

// import t from '../locale/translate'
// import Icon from '../components/Icons'
import {
  // Div,
  Row,
  Col,
  // Button,
  // FormGroup,
  // InputText,
  FormField
} from '../../components/Bootstrap'
import StatefulForm from '../../components/Bootstrap/StatefulForm'
import { MainWithSidebar } from '../../components/Layout'
import Loading from '../../components/Loading'

import { examples as BootstrapExamples } from '../../components/Bootstrap/Bootstrap.examples'
import { examples as MultiSelectExamples } from '../../components/Bootstrap/DownshiftMultiSelect.examples'
import { allIcons } from '../../components/Icons.examples'

// # DATA

const PAGES = [
  {
    id: 'bootstrap',
    title: 'Bootstrap',
    content: BootstrapExamples.map(({ name, content }, i) => (
      <F key={i}>
        <h4>{name}</h4>
        {content}
        <hr />
      </F>
    ))
  },
  {
    id: 'icons',
    title: 'Icons',
    content: allIcons
  },
  {
    id: 'loading-indicator',
    title: 'Loading indicator',
    content: (
      <F>
        <p>default size</p>
        <hr />
        <Loading />
        <hr />
        <p>sizes 1, 2, 3, 4, 5, 6</p>
        <hr />
        <Loading size="1" />
        <hr />
        <Loading size="2" />
        <hr />
        <Loading size="3" />
        <hr />
        <Loading size="4" />
        <hr />
        <Loading size="5" />
        <hr />
        <Loading size="6" />
        <hr />
      </F>
    )
  },
  {
    id: 'stateful-form',
    title: 'Stateful Form',
    content: (
      <F>
        <code>{'<StatefulForm/>'}</code> simple key/value
        <hr />
        <StatefulForm idPrefix="mock-form" values={{ foo: '', bar: '' }}>
          {({ fields, formPropsFor }) => {
            return (
              <F>
                <form
                  id="mock-form"
                  onSubmit={e => {
                    e.preventDefault()
                    window.alert(JSON.stringify(fields, 0, 2))
                  }}
                >
                  <Row>
                    <Col>
                      <FormField label="foo" {...formPropsFor('foo')} />
                      <FormField label="bar" {...formPropsFor('bar')} />
                    </Col>
                  </Row>
                </form>
                <pre>
                  <code>{JSON.stringify(fields, 0, 2)}</code>
                </pre>
              </F>
            )
          }}
        </StatefulForm>
        <hr />
        <code>{'<StatefulForm/>'}</code> nested objects, initial values
        <hr />
        <StatefulForm
          idPrefix="mock-form"
          values={{ one: { foo: '1', bar: 'a' }, two: { foo: '2', bar: 'b' } }}
        >
          {({ fields, formPropsFor }) => {
            return (
              <F>
                <form
                  id="mock-form"
                  onSubmit={e => {
                    e.preventDefault()
                    window.alert(JSON.stringify(fields, 0, 2))
                  }}
                >
                  <Row>
                    <Col>
                      <FormField label="one foo" {...formPropsFor('one.foo')} />
                      <FormField label="one bar" {...formPropsFor('one.bar')} />
                    </Col>
                    <Col>
                      <FormField label="two foo" {...formPropsFor('two.foo')} />
                      <FormField label="two bar" {...formPropsFor('two.bar')} />
                    </Col>
                  </Row>
                </form>
                <pre>
                  <code>{JSON.stringify(fields, 0, 2)}</code>
                </pre>
              </F>
            )
          }}
        </StatefulForm>
      </F>
    )
  },
  {
    id: 'multiselect',
    title: 'MultiSelect',
    content: MultiSelectExamples.map(({ title, content }, i) => (
      <F key={i}>
        <h4>{title}</h4>
        {content}
        <hr />
      </F>
    ))
  }
]

// # PAGE
//
const UiPlayground = ({ match, location }) => {
  const baseUrl = match.url
  const flashMsg = f.get(location, 'state.flash')
  return (
    <MainWithSidebar sidebar={<TableofContents baseUrl={baseUrl} />}>
      {!!flashMsg && (
        <div className="alert alert-warning" role="alert">
          {flashMsg}
        </div>
      )}

      <Switch>
        <Route
          path={`${match.url}/:pageId`}
          render={p => <PageById {...p} baseUrl={baseUrl} />}
        />
        <Route
          exact
          path={match.url}
          render={() => 'select a page from the menu'}
        />
      </Switch>
    </MainWithSidebar>
  )
}

export default UiPlayground

// # PARTIALS

const titleOrById = (title, id) => title || String(id).toUpperCase()

const NavItem = p => (
  <li className="nav-item">
    <NavLink className="nav-link" activeClassName="text-dark" {...p} />
  </li>
)

const TableofContents = ({ baseUrl }) => (
  <ul className="p-2 nav flex-md-column">
    <NavItem to={baseUrl}>
      <h5>Playground</h5>
    </NavItem>
    {PAGES.map(({ id, title }) => (
      <NavItem key={id} to={id ? `${baseUrl}/${id}` : baseUrl}>
        {titleOrById(title, id)}
      </NavItem>
    ))}
  </ul>
)

const PageById = ({ match, baseUrl }) => {
  const { pageId } = match.params
  const page = f.find(PAGES, { id: pageId })
  if (!page) {
    return (
      <Redirect
        to={{
          pathname: baseUrl,
          state: { flash: `The page ${pageId} doesn't exist!` }
        }}
      />
    )
  }
  return (
    <div>
      <h3>{titleOrById(page.title, pageId)}</h3>
      {page.content}
    </div>
  )
}
