import React, { Fragment as F } from 'react'
import f from 'lodash'

import Icon from './Icons'

export const allIcons = f.map(Icon, (Icon, name) => (
  <F key={name}>
    <h4>
      <pre>{`<Icon.${name} />`}</pre>
    </h4>
    <Icon size="3x" />
    <hr />
  </F>
))

export const examples = [
  {
    name: 'usage',
    content: (
      <React.Fragment>
        <Icon.LeihsProcurement size="2x" />{' '}
        <Icon.LeihsProcurement size="2x" color="dark" />{' '}
        <Icon.LeihsProcurement size="2x" color="secondary" />{' '}
        <Icon.LeihsProcurement size="2x" color="light" />{' '}
        <Icon.LeihsProcurement size="2x" color="primary" />{' '}
        <Icon.LeihsProcurement size="2x" color="success" />{' '}
        <Icon.LeihsProcurement size="2x" color="warning" />{' '}
        <Icon.LeihsProcurement size="2x" color="danger" />{' '}
        <Icon.LeihsProcurement size="2x" color="info" />
        <br />
      </React.Fragment>
    )
  },
  { name: 'all icons', content: allIcons }
]
