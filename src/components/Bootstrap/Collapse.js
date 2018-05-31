import React from 'react'
import PropTypes from 'prop-types'
import Icon from '../Icons'

export class Collapse extends React.Component {
  static defaultProps = { startOpen: false, canToggle: true }
  static propTypes = { id: PropTypes.string.isRequired }

  constructor(props) {
    super()
    this.state = { isOpen: props.canToggle === false || props.startOpen }
  }

  onToggleOpen(event) {
    event.preventDefault()
    if (!this.props.canToggle) return
    this.setState(s => ({ isOpen: !s.isOpen }))
  }

  render({ props: { id, children, canToggle }, state: { isOpen } } = this) {
    const toggleOpen = e => this.onToggleOpen(e)
    const collapsedProps = {
      id: `${id}-content`,
      'aria-labelledby': id ? `${id}-toggle` : false
    }
    return children({
      canToggle,
      toggleOpen,
      isOpen,
      Caret: isOpen ? Icon.CaretDown : Icon.CaretRight,
      collapsedProps,
      togglerProps: {
        onClick: toggleOpen,
        id: `${id}-toggle`,
        'aria-expanded': isOpen ? 'true' : 'false',
        'aria-controls': `${id}-content`
      }
    })
  }
}
