import React from 'react'
import {
  Route as RRRoute,
  Redirect as RRRedirect,
  Link
} from 'react-router-dom'
import { RouteParams } from './Bootstrap/Navs'

export { RouteParams, Link }

// sets HTTP status for server-side render
export const RoutedStatus = ({ code, children, ...p }) => (
  <RRRoute {...p}>
    {({ staticContext }) => {
      if (staticContext) staticContext.status = code
      return children
    }}
  </RRRoute>
)

// like ReactRouter.Redirect but with extra features
export const Redirect = ({ scrollTop = true, to, ...props }) => {
  return <RRRedirect to={to} />
}
