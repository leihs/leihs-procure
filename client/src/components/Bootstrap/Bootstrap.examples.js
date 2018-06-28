import React from 'react'

import { Button } from './Bootstrap'

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
  }
]
