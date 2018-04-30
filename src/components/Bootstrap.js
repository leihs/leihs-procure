import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'

import Icon from './Icons'

import { Button as BsButton } from 'reactstrap'

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
window.React = React

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
  const Tag = tag
  const [bsClasses, restProps] = bsSizeUtils(props)
  return <Tag {...restProps} className={bsClasses} />
}

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
    ? breakpointVal ? `col-${breakpoint}-${breakpointVal}` : `col-${breakpoint}`
    : 'col'
  const orderCls =
    order && (breakpoint ? `order-${breakpoint}-${order}` : `order-${order}`)
  return Node({ ...restProps, cls: cx(colCls, orderCls, cls) })
}

export const Button = ({ flat, className, ...props }) => (
  <BsButton {...props} className={cx(className, { 'btn-flat': flat })} />
)

export const Badge = props => {
  const restProps = f.omit(props, BOOTSTRAP_MODIFIERS)
  const mod =
    f.first(f.intersection(f.keys(props), BOOTSTRAP_MODIFIERS)) ||
    BOOTSTRAP_MODIFIERS[0]
  return <Span {...restProps} cls={[props.cls, 'badge', `badge-${mod}`]} />
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

export const FormField = ({
  beforeInput,
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
  let tag = 'input'
  if (!f.includes(supportedTypes, type)) {
    throw new Error('Unsupported Input Type!')
  }

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

  const inputNode =
    // if a node was given as children, use it.
    // if it is an input node, merge in extra props
    // – this is important in case `value` and `onChange` are only given to parent.
    React.isValidElement(children) ? (
      f.includes(children.type, ['input', 'textarea']) ? (
        React.cloneElement(children, inputProps, children.children || value)
      ) : (
        children
      )
    ) : type === 'text-static' ? (
      <Span cls="form-control-plaintext">{value}</Span>
    ) : (
      <Node
        tag={tag}
        autoComplete="off"
        {...inputProps}
        type={type}
        id={id}
        name={name}
        value={value}
        placeholder={placeholder}
        aria-describedby={helpText ? `${id}--Help` : null}
        className="form-control"
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
    </FormGroup>
  )
}

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
    <label className="btn btn-sm btn-block btn-flat btn-outline-secondary text-left">
      <Icon.Paperclip /> {label}
      <input id={id} name={name} type="file" style={{ opacity: 0, width: 0 }} />
    </label>
  </div>
)

export const Select = ({
  options,
  emptyOption = { children: '---', value: '' },
  ...props
}) => {
  return (
    <Node tag="select" className="custom-select" {...props}>
      {emptyOption && <option {...emptyOption} />}
      {options.map(({ label, children, ...props }, i) => (
        <option key={props.id || i} {...props}>
          {children || label}
        </option>
      ))}
    </Node>
  )
}

// TODO: responsive when long labels (e.g. btn-group-vertical)
// https://getbootstrap.com/docs/4.0/components/buttons/#checkbox-and-radio-buttons
// TODO: a11y
// https://inclusive-components.design/toggle-button/
// https://www.w3.org/TR/2016/WD-wai-aria-practices-1.1-20160317/examples/radio/radio.html
export class ButtonRadio extends React.PureComponent {
  getSelectedValueProp(value, selected) {
    // `value` or `selected` is accepted, depending on consistency
    // with DOM or between coomponents is desired.
    if (value && selected) {
      throw new Error(
        'Props `value` and `selected` were given, please only use 1 of them!'
      )
    }
    return value || selected
  }

  render({ props } = this) {
    const {
      id,
      name,
      value,
      selected,
      onChange,
      options = [],
      withIcons = true,
      ...restProps
    } = props
    const selectedValue = this.getSelectedValueProp(value, selected)
    return (
      <div
        {...restProps}
        className="btn-group btn-group-block"
        role="radiogroup"
      >
        {options.map(({ label, value, ...item }, n) => {
          const inputID = `${id}_radio_${n}`
          const isSelected = value === selectedValue
          return (
            <F key={n}>
              <Label
                cls={
                  ('btn btn-block btn-flat btn-outline-secondary m-0 text-left font-weight-bold',
                  {
                    'border-left-0': n !== 0,
                    active: isSelected
                  })
                }
                htmlFor={inputID}
                tabIndex="-1"
              >
                <input
                  {...item}
                  id={inputID}
                  name={name}
                  value={value}
                  type="radio"
                  aria-checked={isSelected}
                  tabIndex={n === 0 ? 0 : -1}
                  onChange={() => {
                    onChange({
                      target: { name: name, value: value }
                    })
                  }}
                  className="sr-only"
                  autoComplete="off"
                />
                {withIcons &&
                  (isSelected ? (
                    <Icon.RadioCheckedOn className="mr-2" />
                  ) : (
                    <Icon.RadioCheckedOff className="mr-2" />
                  ))}
                {label}
              </Label>
            </F>
          )
        })}
      </div>
    )
  }
}
