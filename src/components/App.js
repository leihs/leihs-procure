import React, { Component, Fragment as F } from 'react'

import Icon from './Icons'

const TITLE = 'Bedarfsermittlung'

const Brand = () => (
  <F>
    <Icon.ProcurementLogo className="mr-2" />
    {TITLE}
  </F>
)

class App extends Component {
  render({ props: { children } } = this) {
    return (
      <F>
        <nav className="navbar navbar-dark bg-dark">
          <span className="navbar-brand h1 mb-0" href="#">
            <Brand />
          </span>
        </nav>
        {children}
      </F>
    )
  }
}

export default App
