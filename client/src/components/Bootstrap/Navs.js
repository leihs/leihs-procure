import React from 'react'

import { Route, NavLink as RouterNavLink } from 'react-router-dom'

import Collapse from 'reactstrap/lib/Collapse'
import Navbar from 'reactstrap/lib/Navbar'
import NavbarToggler from 'reactstrap/lib/NavbarToggler'
import BsNavbarBrand from 'reactstrap/lib/NavbarBrand'
import Nav from 'reactstrap/lib/Nav'
import NavItem from 'reactstrap/lib/NavItem'
import BsNavLink from 'reactstrap/lib/NavLink'
import UncontrolledDropdown from 'reactstrap/lib/UncontrolledDropdown'
import DropdownToggle from 'reactstrap/lib/DropdownToggle'
import DropdownMenu from 'reactstrap/lib/DropdownMenu'
import DropdownItem from 'reactstrap/lib/DropdownItem'

export { Collapse }
export { Navbar }
export { NavbarToggler }
export { BsNavbarBrand }
export { Nav }
export { NavItem }
export { BsNavLink }
export { UncontrolledDropdown }
export { DropdownToggle }
export { DropdownMenu }
export { DropdownItem }

export const NavLink = p => (
  <BsNavLink activeClassName="active" tag={RouterNavLink} {...p} />
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

// like ReactRouter.NavLink but for anything that wants to know if its "active".
export const Routed = ({ children, ...p }) => (
  <Route {...p}>{({ match }) => children({ isActive: !!match })}</Route>
)
