import React from 'react'
// import cx from 'classnames'
// import f from 'lodash'

import { DateTime } from 'luxon'
import DayPickerInput from 'react-day-picker/DayPickerInput'
import { DateUtils } from 'react-day-picker'
// TODO: import globally?
import 'react-day-picker/lib/style.css'

const formatDate = date => {
  return DateTime.fromJSDate(date).toLocaleString(DateTime.DATE_SHORT)
}

const parseDate = (str, format, locale) => {
  const parsed = DateTime.fromISO(str)
  if (DateUtils.isDate(parsed)) {
    return parsed
  }
}

const defaultProps = { onChange: () => {} }

const DatePicker = ({
  name,
  value,
  required,
  onChange,
  inputProps,
  ...dayPickerProps
}) => (
  <DayPickerInput
    {...dayPickerProps}
    inputProps={{ className: 'form-control', name, required, ...inputProps }}
    value={!value ? null : DateTime.fromISO(value).toJSDate()}
    placeholder={''}
    formatDate={formatDate}
    parseDate={parseDate}
    onDayChange={day =>
      onChange({ target: { value: day.toISOString(), name } })
    }
  />
)

DatePicker.defaultProps = defaultProps

export default DatePicker
