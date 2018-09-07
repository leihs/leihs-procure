import React, { Fragment as F } from 'react'
import { Route, Switch, NavLink } from 'react-router-dom'
// import cx from 'classnames'
import f from 'lodash'

// import t from '../locale/translate'
// import Icon from '../components/Icons'
import {
  // Div,
  Row,
  Col,
  // Button,
  FormGroup,
  // InputText,
  FormField,
  InputFileUpload
} from '../../components/Bootstrap'
import StatefulForm from '../../components/Bootstrap/StatefulForm'
import { MainWithSidebar } from '../../components/Layout'
import Loading from '../../components/Loading'
import UserAutocomplete from '../../components/UserAutocomplete'
import SupplierAutocomplete from '../../components/SupplierAutocomplete'
import ModelAutocomplete from '../../components/ModelAutocomplete'
import { Redirect } from '../../components/Router'

import { examples as BootstrapExamples } from '../../components/Bootstrap/Bootstrap.examples'
import { examples as MultiSelectExamples } from '../../components/Bootstrap/MultiSelect.examples'
import { examples as IconExamples } from '../../components/Icons.examples'
const Let = ({ children, ...props }) => children(props)

const ExamplesList = ({ examples }) =>
  examples.map(({ name, content }, i) => (
    <F key={i}>
      <h4>{name}</h4>
      {content}
      <hr />
    </F>
  ))

// # DATA

const PAGES = [
  {
    id: 'bootstrap',
    title: 'Bootstrap',
    content: <ExamplesList examples={BootstrapExamples} />
  },
  {
    id: 'icons',
    title: 'Icons',
    content: <ExamplesList examples={IconExamples} />
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
  },
  {
    id: 'autocompletes',
    title: 'Autocompletes',
    content: [
      {
        title: 'User',
        content: (
          <UserAutocomplete
            onSelect={o => window.alert(JSON.stringify(o, 0, 2))}
          />
        )
      },
      {
        title: 'Model',
        content: (
          <ModelAutocomplete
            onSelect={o => window.alert(JSON.stringify(o, 0, 2))}
          />
        )
      },
      {
        title: 'Supplier',
        content: (
          <SupplierAutocomplete
            onSelect={o => window.alert(JSON.stringify(o, 0, 2))}
          />
        )
      }
    ].map(({ title, content }, i) => (
      <F key={i}>
        <h4>{title}</h4>
        {content}
        <hr />
      </F>
    ))
  },
  {
    id: 'input-file-upload',
    title: 'InputFileUpload',
    content: (
      <F>
        <h5>without form</h5>
        <form>
          <FormGroup label="attach a file">
            <InputFileUpload id="example-input-file-upload" />
          </FormGroup>
        </form>
        <hr />
        <h5>with form (shows form data)</h5>
        <StatefulForm idPrefix="input-file-upload-mock-form">
          {({ fields, formPropsFor }) => {
            return (
              <F>
                <form
                  id="input-file-upload-mock-form"
                  onSubmit={e => {
                    e.preventDefault()
                    window.alert(JSON.stringify(fields, 0, 2))
                  }}
                >
                  <InputFileUpload {...formPropsFor('exampleAttachments')} />
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
    id: 'redirect-and-scroll-to-top',
    title: 'Redirect And Scroll To Top',
    content: (
      <Let btnId={'button-way-down-on-page'}>
        {({ btnId }) => (
          <StatefulForm idPrefix="input-file-upload-mock-form">
            {({ getValue, setValue }) => {
              return (
                <F>
                  <p style={{ marginBottom: '300vh' }}>
                    <a href={`#${btnId}`}>scroll down first!</a>
                  </p>
                  <button
                    type="button"
                    id={btnId}
                    onClick={() => setValue('redirect', true)}
                    style={{ marginBottom: '50vh' }}
                  >
                    click to redirect!
                  </button>
                  {!!getValue('redirect') && (
                    <Redirect
                      push
                      scrollTop
                      to={{
                        pathname: '/',
                        state: {
                          flash: {
                            level: 'success',
                            message: 'Should have scrolled to top!'
                          }
                        }
                      }}
                    />
                  )}
                </F>
              )
            }}
          </StatefulForm>
        )}
      </Let>
    )
  }
]

// # PAGE
//
const UiPlayground = ({ match, location }) => {
  const baseUrl = match.url
  const flashMsg = f.get(location, 'state._flash')
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
          state: { _flash: `The page ${pageId} doesn't exist!` }
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
