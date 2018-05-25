import React from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import fpSet from 'lodash/fp/set'
import logger from 'debug'
const log = logger('app:ui:ControlledForm')

// NOTE: handling of getDerivedStateFromProps is dependent on keeping the
//       given props in state as well!
//       explanations: <https://github.com/reactjs/reactjs.org/issues/721>

// # ControlledForm ############################################################
// state container to handle a flat form like in plain HTML.
// input fields use `name`, `value` and `onChange`.
//
// 'name' is interpreted as a object key path in case nested data is nedded
// (`user.name=ann` == {user:{name: 'ann'}})
//
// actual form is rendered by consumer using the `render` prop,
// which will be called with the fields, a callback, and helper.
// helper `formPropsFor` is recommended for normal usage,
// `fields`, `connectFormProps`, `onChange` are given as well for customizations.
export default class ControlledForm extends React.PureComponent {
  static defaultProps = { children: () => {}, onChange: () => {} }
  static propTypes = {
    values: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    onChange: PropTypes.func.isRequired,
    children: PropTypes.func.isRequired
  }

  constructor(props) {
    super()
    this.state = { fields: props.values, originalValues: props.values }
    this.handleInputChange = this.handleInputChange.bind(this)
    this.updateField = this.updateField.bind(this)
  }

  // if new values given via props, reset internal fields state
  static getDerivedStateFromProps(nextProps, prevState) {
    if (nextProps.values === prevState.originalValues) return null
    if (nextProps.values === prevState.fields) return null
    return { fields: nextProps.values, originalValues: nextProps.values }
  }

  handleInputChange(event) {
    this.updateField(getFieldFromEvent(event), fields => {
      if (this.props.onChange) {
        this.props.onChange(fields)
      }
    })
  }

  updateField({ name, value }, callback) {
    log('updateField', { name, value })
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
      const setValue = (name, value) => this.updateField({ name, value })

      return {
        formPropsFor: name => ({
          name,
          id: !idPrefix ? name : `${idPrefix}.${name}`,
          value: getValue(name),
          onChange: this.handleInputChange
        }),
        getValue,
        setValue
      }
    }
    const connected = connectFormProps(state.fields)
    return props.children({
      ...connected,
      fields: state.fields,
      connectFormProps: connectFormProps,
      onChange: this.handleInputChange
    })
  }
}

// # ControlledInput ###########################################################
// its more performant to keep the state in the field,
// and only "proxy" the change events with a slight debounce
export class ControlledInput extends React.PureComponent {
  static defaultProps = { value: '', onChange: () => {} }
  static propTypes = {
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    onChange: PropTypes.func.isRequired,
    children: PropTypes.func.isRequired
  }

  constructor(props) {
    super()
    this.state = { value: props.value, originalValue: props.value }
    this.onChange = this.onChange.bind(this)
    this.onChangeCallbackDebounced = f.debounce(props.onChange, 100)
  }

  // if new value given via props, reset internal state
  static getDerivedStateFromProps(nextProps, prevState) {
    if (nextProps.values === prevState.originalValues) return null
    if (nextProps.values === prevState.fields) return null
    return { value: nextProps.value, originalValue: nextProps.value }
  }

  onChange(event) {
    const callback = this.onChangeCallbackDebounced
    const { value } = event.target
    this.setState(
      state => (state.value === value ? false : { value }),
      () =>
        f.isFunction(callback) &&
        callback({ target: { name: this.props.name, value: this.state.value } })
    )
  }

  render({ props: { children, ...restProps }, state } = this) {
    if (!f.isFunction(children)) {
      throw new Error(
        'ControlledInput needs a single render function as `children`!'
      )
    }
    return children({
      ...restProps,
      value: state.value,
      onChange: this.onChange
    })
  }
}

// ## helpers ##################################################################

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
