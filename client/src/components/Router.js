import React from 'react'
import f from 'lodash'
import { Redirect as RRRedirect } from 'react-router-dom'
import { RouteParams } from './Bootstrap/Navs'

export { RouteParams }

export const Redirect = ({ scrollTop = true, to, ...props }) => (
  <RouteParams>
    {({ location }) => {
      // handle to=path shortcut
      if (f.isString(to)) to = { pathname: to }

      // keep flash in state
      let givenState = f.get(props, 'to.state')
      const flash = f.pick(location.state, 'flash', '_flash')
      if (!f.isEmpty(flash)) to = { ...to, state: { ...givenState, ...flash } }

      // handle scrolling to top
      if (scrollTop && window) {
        setTimeout(() => window.scrollTo(0, 0), 1)
      }

      return <RRRedirect {...props} to={to} />
    }}
  </RouteParams>
)
