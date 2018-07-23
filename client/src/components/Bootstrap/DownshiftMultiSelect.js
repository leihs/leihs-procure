import React from 'react'
import cx from 'classnames'
// import Downshift from 'downshift'

// NOTE: work in progress, right now only "vanilla DOM" with option groups.
//       props/API will stay the same even if implemented using Downshift.

// import ButtonDropdown from 'reactstrap/lib/ButtonDropdown'
// import DropdownToggle from 'reactstrap/lib/DropdownToggle'
// import DropdownMenu from 'reactstrap/lib/DropdownMenu'
// import DropdownItem from 'reactstrap/lib/DropdownItem'

import { getSelectedValueFromProps, getSelectedValueFromEvent } from './Select'

const MultiSelect = ({
  options,
  value,
  onChange,
  readOnly,
  multiple = true,
  className,
  ...props
}) => {
  if (!multiple) {
    throw new Error('MultiSelect only supports `multiple=true`!')
  }

  if (onChange && !props.name) {
    throw new Error('Input with `onChange` needs a `name`!')
  }

  const selectedValue = getSelectedValueFromProps(value, multiple)

  const cls = cx('form-control', className)

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

export default MultiSelect

// function titleBySelection(selected, values) {
//   const count = selected.length
//   const valuesCount = values.length
//   if (count === valuesCount) {
//     return 'Alle ausgew\xE4hlt'
//   }
//   if (count === 0) {
//     return 'Keine ausgew\xE4hlt'
//   }
//   if (count > 3) {
//     return `${count} selektiert`
//   }
//   return selected
//     .map(item => values.filter(({ value }) => value === item)[0])
//     .map(item => item && item.label)
//     .join(', ')
// }
