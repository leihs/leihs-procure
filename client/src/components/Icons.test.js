// import React, { Fragment as F } from 'react'
import renderer from 'react-test-renderer'
import { examples } from './Icons.examples'
import { it, expect } from 'vitest'

examples.forEach(({ name, content }) => {
  it(`${name}: renders correctly`, () => {
    expect(renderer.create(content).toJSON()).toMatchSnapshot()
  })
})
