import React, { Component, Fragment as F } from 'react'

import Icon from './Icons'
import RequestForm from './RequestForm'

const Brand = () => (
  <F>
    <Icon.ProcurementLogo className="mr-2" />Bedarfsermittlung
  </F>
)

class App extends Component {
  render() {
    return (
      <F>
        <nav className="navbar navbar-dark bg-dark">
          <span className="navbar-brand h1 mb-0" href="#">
            <Brand />
          </span>
        </nav>
        <div className="App-body p-3 m-2 border rounded">
          <RequestForm />
        </div>
      </F>
    )
  }
}

export default App
