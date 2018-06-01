import React from 'react'
import PropTypes from 'prop-types'
import cx from 'classnames'

import { __Node as Node } from './Bootstrap'

const Select = ({ options, multiple, emptyOption, value, ...props }) => {
  const selectedValue =
    typeof value === 'object'
      ? multiple
        ? value
        : value[0]
      : multiple
        ? [value]
        : value
  if (emptyOption === true) emptyOption = Select.defaultProps.emptyOption
  return (
    <Node
      tag="select"
      {...props}
      className={cx('custom-select', props.className)}
      multiple={multiple}
      value={selectedValue}
    >
      {emptyOption && <option {...emptyOption} />}
      {options.map(({ label, children, ...props }, ix) => (
        <option key={ix} {...props}>
          {children || label}
        </option>
      ))}
    </Node>
  )
}

Select.defaultProps = {
  multiple: false,
  options: [],
  emptyOption: { children: '---', value: '' }
}

Select.propTypes = {
  id: PropTypes.string,
  className: PropTypes.string,
  multiple: PropTypes.bool,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.arrayOf(PropTypes.string),
    PropTypes.arrayOf(PropTypes.number)
  ]),
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
        .isRequired,
      label: PropTypes.node.isRequired
    })
  ),
  emptyOption: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.shape({
      children: PropTypes.string.isRequired,
      value: PropTypes.string
    })
  ])
}

export default Select
