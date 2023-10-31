import React from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'
import f from 'lodash'
import { isNonEmptyValue } from '../../lib/utils'

import Icon from '../Icons'

const InputTextSearch = ({
  size,
  label,
  name,
  value: inputValue,
  onChange,
  clearLabel,
  className
}) => {
  console
  // const value = f.isString(inputValue) && f.presence(inputValue)

  const value =
    typeof inputValue === 'string' && isNonEmptyValue(inputValue)
      ? inputValue
      : null

  const onClear = e => onChange({ target: { name, value: '' } })
  return (
    <div
      className={cx(className, 'input-group', {
        'input-group-sm': size === 'sm',
        'input-group-lg': size === 'lg'
      })}
    >
      <div className="input-group-prepend">
        <span className="input-group-text" title={label}>
          <Icon.Search />
        </span>
      </div>
      <input
        type="text"
        className="form-control"
        autoComplete="off"
        placeholder={label}
        aria-label={label}
        name={name}
        value={value || ''}
        onChange={onChange}
      />
      <div className="input-group-append">
        <button
          type="button"
          className="btn btn-outline-secondary"
          disabled={!value}
          onClick={onClear}
          title={clearLabel}
          aria-label={clearLabel}
        >
          <Icon.Cross />
        </button>
      </div>
    </div>
  )
}

export default InputTextSearch

InputTextSearch.defaultProps = {
  value: '',
  onChange: f.noop,
  label: 'Suchen…',
  clearLabel: 'Suche zurücksetzen'
}

InputTextSearch.propTypes = {
  name: PropTypes.string,
  value: PropTypes.string,
  onChange: PropTypes.func,
  label: PropTypes.string,
  clearLabel: PropTypes.string
}
