import React from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'
// import f from 'lodash'

import { getSelectedValueFromProps, getSelectedValueFromEvent } from './Select'

// plain/"Vanilla DOM" version

export const MultiSelectPlain = ({
  options,
  value,
  onChange,
  readOnly,
  multiple = true,
  className,
  ...props
}) => {
  const cls = cx('form-control', className)
  const selectedValue = getSelectedValueFromProps(value, multiple)

  if (!multiple) throw new Error('MultiSelect only supports `multiple=true`!')
  if (onChange && !props.name)
    throw new Error('Input with `onChange` needs a `name`!')

  return (
    <select
      multiple
      value={selectedValue}
      className={cls}
      disabled={readOnly}
      onChange={e => {
        if (props.readOnly) return e.preventDefault()
        onChange({
          target: {
            name: props.name,
            value: getSelectedValueFromEvent(e, multiple)
          }
        })
      }}
      {...props}
    >
      {options.map(({ label, options }, i) => (
        <optgroup key={i} label={label}>
          {options.map(({ label, ...props }, i) => (
            <option key={i} {...props}>
              {label}
            </option>
          ))}
        </optgroup>
      ))}
    </select>
  )
}

MultiSelectPlain.defaultProps = {
  multiple: true
}
MultiSelectPlain.propTypes = {
  multiple: PropTypes.oneOf([true]).isRequired,
  name: PropTypes.string,
  onChange: PropTypes.func
}
