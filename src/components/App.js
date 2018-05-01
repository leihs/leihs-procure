import React, { Component, Fragment as F } from 'react'
import { Link } from 'react-router-dom'
import { withRouter } from 'react-router'

import Icon from './Icons'

const TITLE = 'Bedarfsermittlung'

const FooterSpacer = () => (
  // spacing on bottom minimizes scrollbar-flickering and helps in "edge" cases (like popups)
  <F>
    <div className="p-5" />
    <div className="p-5" />
  </F>
)

const Brand = () => (
  <F>
    <Icon.ProcurementLogo className="mr-2" />
    {TITLE}
  </F>
)

const Navbar = () => (
  <nav className="navbar navbar-dark bg-dark">
    <span className="navbar-brand h1 mb-0" href="#">
      <Brand />
      <Link to="/" className="">
        home
      </Link>
      <Link to="/admin/users" className="">
        admin
      </Link>
    </span>
  </nav>
)

const NavbarWithRouter = withRouter(Navbar)

class App extends Component {
  render({ props: { children } } = this) {
    return (
      <F>
        <NavbarWithRouter />
        {children}
        <FooterSpacer />
      </F>
    )
  }
}

export default App
