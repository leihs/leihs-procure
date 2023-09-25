import React from 'react'
// import { Prompt } from 'react-router-dom'
import t from '../locale/translate'

export const initialState = { isBlocking: false }
export const block = (comp) => {
  comp.setState({ isBlocking: true })
}
export const unblock = (comp) => {
  comp.setState({ isBlocking: false })
}

const blockBrowser = () => (window.onbeforeunload = () => true)
const unblockBrowser = () => (window.onbeforeunload = undefined)

export class ConfirmFormNav extends React.Component {
  UNSAFE_componentWillMount() {
    if (this.props.when) {
      blockBrowser()
    }
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (this.props.when) {
      blockBrowser()
    } else {
      unblockBrowser()
    }
  }

  componentWillUnmount() {
    unblockBrowser()
  }

  render() {
    const { when } = this.props
    return (
      <></>
      // <Prompt
      //   when={when}
      //   message={location => t('ui.form.confirm_navigation_when_editing')}
      // />
    )
  }
}
