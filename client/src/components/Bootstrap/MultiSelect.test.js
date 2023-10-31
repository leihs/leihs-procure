import { it, expect } from 'vitest'
import renderer from 'react-test-renderer'
import { examples } from './MultiSelect.examples'

examples.forEach(({ name, content }) => {
  it(`${name}: renders correctly`, () => {
    expect(renderer.create(content).toJSON()).toMatchSnapshot()
  })
})
