import React, { useState, useEffect } from 'react'
import { format } from 'date-fns'
import { Input } from 'reactstrap'
import cx from 'classnames'

function InputDate({ className, readOnly, ...props }) {
  // initialize date to today if not set
  useEffect(() => {
    const date = new Date()

    if (!!!props.value) {
      props.onChange({
        target: { name: props.name, value: date.toISOString() }
      })
    }
  }, [])

  return (
    <div>
      <Input
        readOnly={readOnly}
        className={cx(className)}
        type="date"
        name={props.name}
        required
        defaultValue={
          !!props.value ? format(Date.parse(props.value), 'y-MM-dd') : ''
        }
        onChange={e => {
          const date = new Date(e.target.value)
          props.onChange({
            target: { name: props.name, value: date.toISOString() }
          })
        }}
      />
    </div>
  )
}

export default InputDate
