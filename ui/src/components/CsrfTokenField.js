import React from 'react'

const CsrfTokenField = ({ name, value, isOptional = false }) => {
  if (!isOptional && !value) throw new Error('CsrfTokenField: `value` prop is required')
  return <input type="hidden" name={name || 'csrf-token'} value={value} />
}

export default CsrfTokenField
