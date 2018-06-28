import React, { Component, Fragment as F } from 'react'
import { NavLink } from 'react-router-dom'
import { withRouter } from 'react-router'

import Icon from './Icons'

const TITLE = 'Bedarfsermittlung'

const Brand = () => (
  <F>
    <Icon.ProcurementLogo className="mr-2" />
    {TITLE}
  </F>
)

const NavItem = p => (
  <NavLink className="nav-link" activeClassName="text-light" {...p} />
)

const Navbar = ({ withPlayground }) => (
  <nav className="navbar navbar-dark bg-dark navbar-expand-sm">
    <span className="navbar-brand h1 mb-0" href="#">
      <Brand />
    </span>
    <div className="navbar-nav">
      <NavItem exact to="/requests">
        requests
      </NavItem>
      <span className="navbar-text">admin</span>
      <NavItem to="/admin/users">users</NavItem>
      <NavItem to="/admin/categories">categories</NavItem>
      <NavItem to="/admin/organizations">organizations</NavItem>
      {!!withPlayground && <NavItem to="/playground">[PLAYGROUND]</NavItem>}
    </div>
  </nav>
)

const NavbarWithRouter = withRouter(Navbar)

class App extends Component {
  render({ props: { children, withPlayground } } = this) {
    return (
      // TODO: set lang to instance default language
      <div className="ui-app" lang="de">
        <NavbarWithRouter withPlayground={withPlayground} />
        {children}
      </div>
    )
  }
}

export default App
