import React from 'react'
import renderer from 'react-test-renderer'

import { Button } from './Bootstrap'

it('Button: renders correctly', () => {
  const tree = renderer.create(
    <React.Fragment>
      <Button>Button</Button>
      <Button color="primary" title="test title">
        Button
      </Button>
      <Button color="link" title="test title" className="my-class">
        Button
      </Button>
    </React.Fragment>
  )
  expect(tree.toJSON()).toMatchSnapshot()
})
