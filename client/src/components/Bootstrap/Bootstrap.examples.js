import React from 'react'

import { Button, Badge } from './Bootstrap'

const _space_ = ' '

export const examples = [
  {
    name: 'Button',
    content: (
      <React.Fragment>
        <Button>Button</Button>
        {_space_}
        <Button color="primary" title="test title">
          Button
        </Button>
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
  }
]
