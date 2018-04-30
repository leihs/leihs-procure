import React from 'react'
import f from 'lodash'
import fpSet from 'lodash/fp/set'
// import qs from 'qs'

// deal with differences in finding the
// current value of input fields
function getFieldFromEvent({ target }) {
  const name = target.name
  const value =
    target.type === 'checkbox'
      ? target.checked
      : target.type === 'radio'
        ? target.selected
        : // text, number, etc:
          target.value

  return { name, value: value || null }
}

// state container to handle a flat form like in plain HTML.
// input fields use `name`, `value` and `onChange`.
// 'name' is interpreted as a object key path in case nested data is nedded
// (`user.name=ann` == {user:{name: 'ann'}})
// actual form is rendered by consumer using the `render` prop,
// which will be called with the fields, a callback, and helper.
// helper `formPropsFor` is recommended for normal usage,
// `fields`, `connectFormProps`, `onChange` are given as well for customizations.
export default class ControlledForm extends React.Component {
  constructor() {
    super()
    this.state = { fields: {} }
    this.handleInputChange = this.handleInputChange.bind(this)
    this.updateField = this.updateField.bind(this)
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if (nextProps.values === prevState.fields) return null
    return { fields: { ...prevState.fields, ...nextProps.values } }
  }

  handleInputChange(event) {
    this.updateField(getFieldFromEvent(event), fields => {
      if (this.props.onChange) {
        this.props.onChange(fields)
      }
    })
  }

  updateField({ name, value }, callback) {
    this.setState(state => {
      return {
        ...state,
        fields: fpSet(name, value, state.fields)
      }
    }, () => f.isFunction(callback) && callback(this.state.fields))
  }

  render({ props, state } = this) {
    const connectFormProps = (fields, opts = {}) => {
      const defaultConf = { idPrefix: this.props.idPrefix }
      const conf = { ...defaultConf, ...opts }
      const { idPrefix } = conf
      const getValue = name => f.get(fields, name) || ''
      return {
        formPropsFor: name => ({
          name,
          id: !idPrefix ? name : `${idPrefix}.${name}`,
          value: getValue(name),
          onChange: this.handleInputChange
        }),
        getValue: name => getValue(name),
        setValue: (name, value) => this.updateField({ name, value })
      }
    }
    const connected = connectFormProps(state.fields)
    return props.render({
      ...connected,
      fields: state.fields,
      connectFormProps: connectFormProps,
      onChange: this.handleInputChange
    })
  }
}
