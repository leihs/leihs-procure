import React from 'react'
import f from 'lodash'
import {
  Route as RRRoute,
  Redirect as RRRedirect,
  Link
} from 'react-router-dom'
import { RouteParams } from './Bootstrap/Navs'
import { parse as parseQuery } from 'qs'

export { RouteParams, Link }

// like ReactRouter.NavLink but for anything that wants to know if its "active".
export const RoutedActive = ({ children, ...p }) => (
  <RRRoute {...p}>{({ match }) => children({ isActive: !!match })}</RRRoute>
)

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
export const Redirect = ({ scrollTop = true, to, ...props }) => (
  <RouteParams>
    {({ location }) => {
      // handle to=path shortcut
      if (f.isString(to)) to = { pathname: to }

      // keep flash in state
      let givenState = f.get(props, 'to.state')
      const flash = f.pick(location.state, 'flash', 'localFlash')
      if (!f.isEmpty(flash)) to = { ...to, state: { ...givenState, ...flash } }

      // handle scrolling to top
      if (scrollTop && window) {
        setTimeout(() => window.scrollTo(0, 0), 1)
      }

      return <RRRedirect {...props} to={to} />
    }}
  </RouteParams>
)

// like ReactRouter.Route but with extra features
export const Routed = ({ children, ...p }) => (
  <RRRoute {...p}>
    {({ location, history, ...routerProps }) => {
      // flash helpers
      const setFlash = flash => setFlashIn(history, 'flash', flash)
      const setLocalFlash = flash => setFlashIn(history, 'localFlash', flash)
      const dismissFlash = flash => dismissFlashIn(history, 'flash')
      const dismissLocalFlash = flash => dismissFlashIn(history, 'localFlash')

      // parse query params into object
      const params = parseQuery(location.search.slice(1))
      return children({
        ...routerProps,
        location,
        history,
        params,
        setFlash,
        dismissFlash,
        setLocalFlash,
        dismissLocalFlash
      })
    }}
  </RRRoute>
)

// helpers

const setFlashIn = (history, key, flash) => {
  const loc = history.location
  history.replace({
    ...loc,
    state: f.presence({ ...loc.state, [key]: { level: 'success', ...flash } })
  })
}
const dismissFlashIn = (history, key) => {
  const loc = history.location
  history.replace({ ...loc, state: { ...f.omit(loc.state, key) } })
}
