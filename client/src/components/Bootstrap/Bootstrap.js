import React, { Fragment as F } from 'react'
import PropTypes from 'prop-types'
import cx from 'classnames'
import f from 'lodash'

import BsButton from 'reactstrap/lib/Button'
import BsButtonGroup from 'reactstrap/lib/ButtonGroup'

import Icon from '../Icons'
import { StatefulInput } from './StatefulForm'

const BOOTSTRAP_BREAKPOINTS = ['sm', 'md', 'lg', 'xl']
const BOOTSTRAP_MODIFIERS = [
  'primary',
  'secondary',
  'success',
  'danger',
  'warning',
  'info',
  'light',
  'dark'
]

// https://getbootstrap.com/docs/4.0/utilities/spacing/#notation
const bsSizeUtils = ({ m, p, ...props }) => {
  const { cls, className, ...restProps } = props
  function extract(letter, prop) {
    if (f.isArray(prop)) return prop.map(p => extract(letter, p))
    if (!f.isString(prop)) return
    let parts = prop.split(' ')
    if (parts.length > 1) return extract(letter, parts)
    return `${letter}${prop}`
  }
  const margin = extract('m', m)
  const padding = extract('p', p)
  return [cx(margin, padding, cls, className), restProps]
}

const Node = ({ tag = 'div', ...props }) => {
  // eslint-disable-next-line no-debugger
  const Tag = tag
  const [bsClasses, restProps] = bsSizeUtils(props)
  return <Tag {...restProps} className={bsClasses} />
}
Node.propTypes = {
  tag: PropTypes.string
}

export { Node as __Node } // "internal" usage only

export const Div = props => Node(props)
export const Pre = props => Node({ ...props, tag: 'span' })
export const Span = props => Node({ ...props, tag: 'span' })
export const Code = props => Node({ ...props, tag: 'span' })
export const Hr = props => Node({ ...props, tag: 'span' })
export const Main = props => Node({ ...props, tag: 'main' })
export const Label = props => Node({ ...props, tag: 'label' })
export const Small = props => Node({ ...props, tag: 'small' })

export const Container = ({ fluid, ...props }) =>
  Node({ ...props, cls: [fluid ? 'container-fluid' : 'container', props.cls] })

export const Row = ({ form, ...props }) =>
  Node({ ...props, cls: [form ? 'form-row' : 'row', props.cls] })

export const Col = ({ order, cls, ...props }) => {
  const restProps = f.omit(props, BOOTSTRAP_BREAKPOINTS)
  const breakpoint = f.first(
    f.intersection(f.keys(props), BOOTSTRAP_BREAKPOINTS)
  )
  const breakpointVal = props[breakpoint]
  const colCls = breakpoint
    ? f.isString(breakpointVal) || f.isNumber(breakpointVal)
      ? `col-${breakpoint}-${breakpointVal}`
      : `col-${breakpoint}`
    : 'col'
  const orderCls =
    order && (breakpoint ? `order-${breakpoint}-${order}` : `order-${order}`)
  return Node({ ...restProps, cls: cx(colCls, orderCls, cls) })
}

export const Button = ({ massive, className, cls, ...props }) => (
  <BsButton
    type="button" // default in case not given, otherwise its 'submit'
    {...props}
    className={cx(className, cls, { 'btn-massive': massive })}
  />
)
Button.propTypes = {
  className: PropTypes.string,
  massive: PropTypes.bool
}

export { BsButtonGroup as ButtonGroup }

export const Badge = props => {
  const restProps = f.omit(props, BOOTSTRAP_MODIFIERS)
  const mod =
    f.first(f.intersection(f.keys(props), BOOTSTRAP_MODIFIERS)) ||
    BOOTSTRAP_MODIFIERS[0]
  return <Span {...restProps} cls={[props.cls, 'badge', `badge-${mod}`]} />
}
Badge.propTypes = {
  cls: PropTypes.any // todo: classnames.proptypes
}

export const FormGroup = ({
  id,
  label,
  hideLabel,
  labelSmall,
  helpText,
  children,
  ...props
}) => {
  const labelContent = !!(label || labelSmall) && (
    <F>
      {label}
      {!!labelSmall && (
        <F>
          {' '}
          <Small>{labelSmall}</Small>
        </F>
      )}
    </F>
  )

  return (
    <Node {...props} cls="form-group" tag="fieldset">
      {labelContent && (
        <Label htmlFor={id} cls={{ 'sr-only': hideLabel }}>
          {labelContent}{' '}
        </Label>
      )}
      {!!children && <Div>{children} </Div>}
      {helpText && (
        <Small id={`${id}--Help`} cls="form-text text-muted">
          {f.trim(helpText)}{' '}
        </Small>
      )}
    </Node>
  )
}
FormGroup.propTypes = {
  id: PropTypes.string,
  label: PropTypes.node,
  hideLabel: PropTypes.bool,
  labelSmall: PropTypes.node,
  helpText: PropTypes.node,
  children: PropTypes.node
}

export const InputText = props => (
  <StatefulInput {...props}>
    {inputProps => (
      <Node
        {...inputProps}
        tag="input"
        type="text"
        cls={['form-control', inputProps.cls]}
        autoComplete={
          props.autoComplete === true // let the browser decide if true, or
            ? null // pass through given prop or turn off if falsy/nothing
            : props.autoComplete || 'off'
        }
      />
    )}
  </StatefulInput>
)

const FormFieldPropTypes = {
  beforeInput: PropTypes.node,
  afterInput: PropTypes.node,
  helpText: PropTypes.node,
  id: PropTypes.string,
  children: PropTypes.oneOf([null, undefined]),
  label: PropTypes.node.isRequired,
  hideLabel: PropTypes.bool,
  labelSmall: PropTypes.node,
  name: PropTypes.string.isRequired,
  placeholder: PropTypes.string,
  type: PropTypes.string, // enum, already checked at runtime
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
}
export const FormField = ({
  beforeInput,
  afterInput,
  helpText,
  id,
  children,
  label,
  hideLabel,
  labelSmall,
  name,
  placeholder,
  type = 'text',
  value,
  autoComplete,
  ...inputProps
}) => {
  const supportedTypes = [
    'text',
    'text-static',
    'textarea',
    'number',
    'number-integer',
    'checkbox'
  ]
  if (children) {
    throw new Error('`children` not supported! Use `FormGroup` instead.')
  }

  if (!f.includes(supportedTypes, type)) {
    throw new Error('Unsupported Input Type!')
  }

  let tag = 'input'
  let mainClass = 'form-control'
  if (!id) id = name
  if (!name) name = id

  if (type === 'number-integer') {
    type = 'number'
    inputProps = { min: 1, step: 1, ...inputProps }
  }

  if (type === 'textarea') {
    tag = type
    type = null
    let [minRows, maxRows] = [3, 7]
    inputProps = {
      rows: Math.max(
        minRows,
        Math.min(maxRows, String(value).split('\n').length)
      ),
      ...inputProps
    }
  }

  if (type === 'text-static') {
    tag = 'span'
    mainClass = 'form-control-plaintext'
  }

  const inputAutoComplete =
    autoComplete === true // let the browser decide if true, or
      ? null // pass through given prop or turn off if falsy/nothing
      : autoComplete || 'off'

  const inputNode = (
    <Node
      aria-describedby={helpText ? `${id}--Help` : null}
      {...inputProps}
      tag={tag}
      cls={mainClass}
      type={type}
      id={id}
      name={name}
      value={value === null ? '' : value}
      autoComplete={inputAutoComplete}
      placeholder={placeholder}
    />
  )

  return (
    <FormGroup
      label={label}
      hideLabel={hideLabel}
      labelSmall={labelSmall}
      helpText={helpText}
    >
      {beforeInput}
      {inputNode}
      {afterInput}
    </FormGroup>
  )
}
FormField.propTypes = FormFieldPropTypes

// like bootstrap docs
// export const FilePicker = ({ id, name }) => (
//   <div className="custom-file">
//     <input type="file" className="custom-file-input" id={id} name={name} />
//     <label className="custom-file-label" htmlFor="customFile">
//       <Icon.Paperclip /> {t('form_filepicker_label')}
//     </label>
//   </div>
// )

export const FilePicker = ({ id, name, label }) => (
  <div>
    <label className="btn btn-sm btn-block  btn-outline-secondary text-left">
      <Icon.Paperclip /> {label}
      <input id={id} name={name} type="file" style={{ opacity: 0, width: 0 }} />
    </label>
  </div>
)
FilePicker.propTypes = {
  id: PropTypes.string,
  name: PropTypes.string,
  label: PropTypes.node
}
