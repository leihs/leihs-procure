import React from 'react'

import { Row, Col, Button, Badge, FormField, InputDate } from './index'

const _space_ = ' '

export const examples = [
  {
    name: 'Button',
    content: (
      <React.Fragment>
        <Button>Button</Button>
        {_space_}
        <Button color="primary" title="test title">
          Button with title
        </Button>
        {_space_}
        <Button color="primary">Button primary</Button>
        {_space_}
        <Button color="secondary">Button secondary</Button>
        {_space_}
        <Button color="success">Button success</Button>
        {_space_}
        <Button color="danger">Button danger</Button>
        {_space_}
        <Button color="warning">Button warning</Button>
        {_space_}
        <Button color="info">Button info</Button>
        {_space_}
        <Button color="light">Button light</Button>
        {_space_}
        <Button color="dark">Button dark</Button>
        {_space_}
        {_space_}
        <Button onClick={e => window.alert('clicked!')}>Button</Button>
        {_space_}
        <Button color="link" title="test title" className="my-class">
          Button
        </Button>
      </React.Fragment>
    )
  },
  {
    name: 'Badge',
    content: (
      <React.Fragment>
        <h3>
          Heading <Badge>Badge</Badge>
        </h3>
        <Badge>Badge</Badge>
        {_space_}
        <Badge primary>Badge primary</Badge>
        {_space_}
        <Badge secondary>Badge secondary</Badge>
        {_space_}
        <Badge success>Badge success</Badge>
        {_space_}
        <Badge danger>Badge danger</Badge>
        {_space_}
        <Badge warning>Badge warning</Badge>
        {_space_}
        <Badge info>Badge info</Badge>
        {_space_}
        <Badge light>Badge light</Badge>
        {_space_}
        <Badge dark>Badge dark</Badge>
      </React.Fragment>
    )
  },
  {
    name: 'FormField',
    content: (() => {
      class DemoForm extends React.Component {
        state = {
          a: 'some text',
          b: 'some static text',
          c: 'some text',
          d: '23.42',
          e: '42',
          f: true
        }
        render() {
          return (
            <Row>
              <Col>
                <FormField
                  type="text"
                  label="type=text"
                  name="a"
                  value={this.state.a}
                  onChange={e => this.setState({ a: e.target.value })}
                />
                <FormField
                  name="b"
                  type="text-static"
                  label="type=text-static"
                  value={this.state.b}
                  onChange={e => this.setState({ b: e.target.value })}
                />
                <FormField
                  name="c"
                  type="textarea"
                  label="type=textarea"
                  value={this.state.c}
                  onChange={e => this.setState({ c: e.target.value })}
                />
              </Col>
              <Col>
                <FormField
                  name="d"
                  type="number"
                  label="type=number"
                  value={this.state.d}
                  onChange={e => this.setState({ d: e.target.value })}
                />
                <FormField
                  name="e"
                  type="number-integer"
                  label="type=number-integer"
                  value={this.state.e}
                  onChange={e => this.setState({ e: e.target.value })}
                />
                <FormField
                  name="f"
                  type="checkbox"
                  label="type=checkbox"
                  checked={this.state.f}
                  onChange={e => this.setState({ f: e.target.value })}
                />
                <FormField
                  name="g"
                  type="checkbox"
                  label="type=checkbox"
                  checked={this.state.g}
                  onChange={e => this.setState({ g: e.target.value })}
                />
              </Col>
              <Col>
                <pre>{JSON.stringify(this.state, 0, 2)}</pre>
              </Col>
            </Row>
          )
        }
      }
      return (
        <React.Fragment>
          <h2>controlled</h2>
          <DemoForm />
          {/* <h2>uncontrolled</h2>
          <Row>
            <Col>
              <FormField
                name="a"
                type="text"
                label="type=text"
                defaultValue="some text"
              />
              <FormField
                name="b"
                type="text-static"
                label="type=text-static"
                defaultValue="some static text"
              />
              <FormField
                name="c"
                type="textarea"
                label="type=textarea"
                defaultValue="some text"
              />
            </Col>
            <Col>
              <FormField
                name="d"
                type="number"
                label="type=number"
                defaultValue="23.42"
              />
              <FormField
                name="e"
                type="number-integer"
                label="type=number-integer"
                defaultValue="42"
              />
              <FormField name="f" type="checkbox" label="type=checkbox" />
              <FormField
                name="g"
                type="checkbox"
                label="type=checkbox defaultChecked={true}"
                defaultChecked={true}
              />
            </Col>
            <Col>
              <pre>(form state kept in DOM)</pre>
            </Col>
          </Row> */}
        </React.Fragment>
      )
    })()
  },
  {
    name: 'InputDate',
    content: (
      <React.Fragment>
        no value
        <br />
        <InputDate />
        <hr />
        with value <code>1985-10-26T08:15:00.000Z</code>
        <br />
        <InputDate value="1985-10-26T08:15:00.000Z" />
      </React.Fragment>
    )
  }
]
