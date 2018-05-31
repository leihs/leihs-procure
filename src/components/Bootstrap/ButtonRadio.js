import React, { Fragment as F } from 'react'
// import PropTypes from 'prop-types'
import cx from 'classnames'
// import f from 'lodash'
import { Label } from './Bootstrap'
import Icon from '../Icons'

// TODO: responsive when long labels (e.g. btn-group-vertical)
// https://getbootstrap.com/docs/4.0/components/buttons/#checkbox-and-radio-buttons
// TODO: a11y
// https://inclusive-components.design/toggle-button/
// https://www.w3.org/TR/2016/WD-wai-aria-practices-1.1-20160317/examples/radio/radio.html
class ButtonRadio extends React.PureComponent {
  getSelectedValueProp(value, selected) {
    // `value` or `selected` is accepted, depending on consistency
    // with DOM or between components is desired.
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
                cls={cx(
                  'btn btn-block btn-flat btn-outline-secondary m-0 text-left font-weight-bold',
                  {
                    'border-left-0': n !== 0,
                    active: isSelected
                  }
                )}
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
                  autoComplete="off-even-in-chrome"
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

export default ButtonRadio
