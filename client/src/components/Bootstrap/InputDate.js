import React from 'react'
import cx from 'classnames'
// import f from 'lodash'

import DayPickerInput from 'react-day-picker/DayPickerInput'
import { DateTime } from 'luxon'
// TODO: use Luxon instead of Moment
import MomentLocaleUtils, {
  formatDate,
  parseDate
} from 'react-day-picker/moment'

// TODO: import globally?
import 'react-day-picker/lib/style.css'

// FIXME: choose locale/language/format
import 'moment/locale/de'
const DATE_LANG = 'de'
const DATE_FMT = 'L'

const defaultProps = { onChange: () => {} }

const DatePicker = ({
  name,
  value,
  required,
  readOnly,
  onChange,
  inputProps = {},
  ...dayPickerProps
}) => {
  // use value as date if valid, otherwise raw string
  const parsedDate = DateTime.fromISO(value)
  const currentValue = parsedDate.isValid ? parsedDate.toJSDate() : value

  return (
    <DayPickerInput
      {...dayPickerProps}
      inputProps={{
        name,
        required,
        readOnly,
        autoComplete: 'off',
        ...inputProps,
        className: cx('form-control', inputProps.className, {
          'is-invalid': !parsedDate.isValid
        })
      }}
      value={currentValue}
      onDayChange={(day, modifiers, dayPickerInput) => {
        // send raw input string if no valid day is selected
        const value = day ? day.toISOString() : dayPickerInput.getInput().value
        onChange({ target: { name, value } })
      }}
      // i18n date format support:
      formatDate={formatDate}
      parseDate={parseDate}
      format={DATE_FMT}
      placeholder={`${formatDate(new Date(2000, 0, 1), DATE_FMT, DATE_LANG)}`}
      dayPickerProps={{
        locale: DATE_LANG,
        localeUtils: MomentLocaleUtils
      }}
    />
  )
}

DatePicker.defaultProps = defaultProps

export default DatePicker
