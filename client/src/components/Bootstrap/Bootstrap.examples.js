import React from 'react'

import { Button, Badge, InputDate } from './index'

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
