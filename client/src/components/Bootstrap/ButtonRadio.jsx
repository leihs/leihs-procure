import React, { Fragment as F } from 'react'
// import PropTypes from 'prop-types'
import cx from 'classnames'
import f from 'lodash'
import { Label } from './Bootstrap'
import Icon from '../Icons'
import { isNonEmptyValue } from '../../lib/utils'

const BASE_CLASS = 'ui-btn-radio'

// TODO: responsive when long labels (e.g. btn-group-vertical)
// https://getbootstrap.com/docs/4.0/components/buttons/#checkbox-and-radio-buttons
// TODO: a11y
// https://inclusive-components.design/toggle-button/
// https://www.w3.org/TR/2016/WD-wai-aria-practices-1.1-20160317/examples/radio/radio.html
class ButtonRadio extends React.PureComponent {
  getSelectedValueProp(value, selected) {
    // `value` or `selected` is accepted, depending on consistency
    // with DOM or between components is desired.
    // if (f.present(value) && f.present(selected)) {
    if (isNonEmptyValue(value) && isNonEmptyValue(selected)) {
      throw new Error(
        'Props `value` and `selected` were given, please only use 1 of them!'
      )
    }
    // return f.present(value) ? value : selected

    return isNonEmptyValue(value) ? value : selected
  }

  isDisabled = (p = this.props) => p.disabled || p.readOnly

  render(
    {
      props: {
        id,
        name,
        required,
        value,
        selected,
        onChange,
        options = [],

        withIcons = true,
        ...restProps
      }
    } = this
  ) {
    const selectedValue = this.getSelectedValueProp(value, selected)
    const isDisabled = this.isDisabled()
    const isInvalid = !!(required && !f.find(options, { value: selectedValue }))

    return (
      <div
        className={cx(
          BASE_CLASS,
          'btn-group btn-group-toggle d-flex',
          isInvalid && `${BASE_CLASS}-state-invalid`
        )}
        role="radiogroup"
        {...restProps}
      >
        {options.map(({ label, value, ...item }, n) => {
          const inputID = `${id}_radio_${n}`
          const isSelected = value === selectedValue
          const onRadioClick = () => {
            if (isDisabled) return false
            // if already selected, a click de-selects!
            onChange({
              target: { name: name, value: isSelected ? null : value }
            })
          }

          return (
            <F key={n}>
              <Label
                cls={cx(
                  'btn btn-block btn-outline-secondary m-0 text-left font-weight-bold',
                  {
                    'border-left-0': n !== 0,
                    active: isSelected,
                    disabled: isDisabled
                  }
                )}
                htmlFor={inputID}
                tabIndex="-1"
              >
                <input
                  // NOTE: hidden, but centered for browser-native validation messages!
                  className="sr-only mt-4 ml-2"
                  tabIndex={n === 0 ? 0 : -1}
                  {...item}
                  type="radio"
                  id={inputID}
                  name={name}
                  value={value}
                  required={required}
                  checked={isSelected}
                  aria-checked={isSelected}
                  onChange={onRadioClick}
                  onClick={onRadioClick}
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

ButtonRadio.defaultProps = {
  onChange: f.noop
}

export default ButtonRadio
