import React from 'react'
import f from 'lodash'
import UncontrolledTooltip from 'reactstrap/lib/UncontrolledTooltip'

const Tooltipped = ({ children, text, delay, ...p }) => {
  const child = React.Children.only(children)
  const targetId = child.props.id

  if (f.isEmpty(targetId) || !f.isString(targetId)) {
    throw new Error('Tooltipped child needs an `id` prop!')
  }

  return (
    <React.Fragment>
      {child}
      <UncontrolledTooltip
        delay={{ show: 100, hide: 0, ...delay }}
        target={targetId}
        {...p}
      >
        {text}
      </UncontrolledTooltip>
    </React.Fragment>
  )
}

export default Tooltipped
