import React from 'react'
import { parse as parseQuery } from 'qs'
import {
  Route as RouterRoute,
  NavLink as RouterNavLink
} from 'react-router-dom'

import Collapse from 'reactstrap/lib/Collapse'
import Navbar from 'reactstrap/lib/Navbar'
import NavbarToggler from 'reactstrap/lib/NavbarToggler'
import BsNavbarBrand from 'reactstrap/lib/NavbarBrand'
import Nav from 'reactstrap/lib/Nav'
import NavItem from 'reactstrap/lib/NavItem'
import BsNavLink from 'reactstrap/lib/NavLink'
import UncontrolledDropdown from 'reactstrap/lib/UncontrolledDropdown'
import ButtonDropdown from 'reactstrap/lib/ButtonDropdown'
import DropdownToggle from 'reactstrap/lib/DropdownToggle'
import DropdownMenu from 'reactstrap/lib/DropdownMenu'
import DropdownItem from 'reactstrap/lib/DropdownItem'

import { Anchor } from '.'

export { Collapse }
export { Navbar }
export { NavbarToggler }
export { BsNavbarBrand }
export { Nav }
export { NavItem }
export { BsNavLink }
export { UncontrolledDropdown }
export { ButtonDropdown }
export { DropdownToggle }
export { DropdownMenu }
export { DropdownItem }

export const NavLink = p => (
  <BsNavLink activeClassName="active" tag={RouterNavLink} {...p} />
)

export const NavItemAnchor = p => (
  <NavItem>
    <BsNavLink tag={Anchor} {...p} />
  </NavItem>
)

export const NavItemLink = p => (
  <NavItem>
    <NavLink {...p} />
  </NavItem>
)

export const NavbarBrand = ({ tag, to, ...p }) => (
  <BsNavbarBrand tag={tag || to ? RouterNavLink : 'span'} to={to} {...p} />
)

export const DropdownItemLink = p => <DropdownItem tag={RouterNavLink} {...p} />

// TODO: move most of those to ../Router:

// like ReactRouter.NavLink but for anything that wants to know if its "active".
export const Routed = ({ children, ...p }) => (
  <RouterRoute {...p}>
    {({ match }) => children({ isActive: !!match })}
  </RouterRoute>
)

// like ReactRouter.Route but additionally with parsed params
export const RouteParams = ({ children, ...p }) => (
  <RouterRoute {...p}>
    {({ location, ...routerProps }) => {
      const params = parseQuery(location.search.slice(1))
      return children({ ...routerProps, location, params })
    }}
  </RouterRoute>
)

// sets HTTP status for server-side render
export const RoutedStatus = ({ code, children, ...p }) => (
  <RouterRoute {...p}>
    {({ staticContext }) => {
      if (staticContext) staticContext.status = code
      return children
    }}
  </RouterRoute>
)
