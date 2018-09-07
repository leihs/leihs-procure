// import React from 'react'
import renderer from 'react-test-renderer'

import { examples } from './Collapsing.examples'

examples.forEach(({ name, content }) => {
  it(`${name}: renders correctly`, () => {
    expect(renderer.create(content).toJSON()).toMatchSnapshot()
  })
})
