import React from 'react'
import { Redirect as RRRedirect } from 'react-router-dom'

export const Redirect = ({ scrollTop = true, ...props }) => {
  if (scrollTop && window) {
    window.scrollTo(0, 0)
  }

  return <RRRedirect {...props} />
}
