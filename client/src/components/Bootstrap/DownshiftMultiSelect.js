import React from 'react'
// import Downshift from 'downshift'

// import ButtonDropdown from 'reactstrap/lib/ButtonDropdown'
// import DropdownToggle from 'reactstrap/lib/DropdownToggle'
// import DropdownMenu from 'reactstrap/lib/DropdownMenu'
// import DropdownItem from 'reactstrap/lib/DropdownItem'

const MultiSelect = ({ options, ...rest }) => {
  return (
    <select multiple {...rest}>
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
