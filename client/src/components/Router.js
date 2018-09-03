import React from 'react'
import { Redirect as RRRedirect } from 'react-router-dom'

export const Redirect = ({ scrollTop = true, ...props }) => {
  if (scrollTop && window) {
    setTimeout(() => window.scrollTo(0, 0), 1)
  }

  return <RRRedirect {...props} />
}
