import React, { Component, Fragment as F } from 'react'
import { Link } from 'react-router-dom'
import { withRouter } from 'react-router'

import Icon from './Icons'

const TITLE = 'Bedarfsermittlung'

const Brand = () => (
  <F>
    <Icon.ProcurementLogo className="mr-2" />
    {TITLE}
  </F>
)

const Navbar = () => (
  <nav className="navbar navbar-dark bg-dark navbar-expand-sm">
    <span className="navbar-brand h1 mb-0" href="#">
      <Brand />
    </span>
    <div className="navbar-nav">
      <Link className="nav-link" to="/">
        requests
      </Link>
      <span className="navbar-text">admin</span>
      <Link className="nav-link" to="/admin/users">
        users
      </Link>
      <Link className="nav-link" to="/admin/categories">
        categories
      </Link>
    </div>
  </nav>
)

const NavbarWithRouter = withRouter(Navbar)

class App extends Component {
  render({ props: { children } } = this) {
    return (
      // TODO: set lang to instance default language
      <div className="ui-app" lang="de">
        <NavbarWithRouter />
        {children}
      </div>
    )
  }
}

export default App
