import React, { Fragment as F } from 'react'
import f from 'lodash'

import Icons from './Icons'

export const allIcons = f.map(Icons, (Icon, name) => (
  <F key={name}>
    <h4>
      <pre>{`<Icon.${name} />`}</pre>
    </h4>
    <Icon size="3x" />
    <hr />
  </F>
))

export const examples = [{ name: 'all icons', content: allIcons }]
