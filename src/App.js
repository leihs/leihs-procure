import React, { Component } from 'react'

import Icon from './Icons'
import RequestForm from './RequestForm'

class App extends Component {
  render() {
    return (
      <div className="App">
        {/* <header className="App-header">
          <h1 className="App-title">
            <Icon.ProcurementLogo /> Bedarfsermittlung
          </h1>
        </header> */}
        <nav className="navbar navbar-dark bg-dark">
          <span className="navbar-brand h1 mb-0" href="#">
            <Icon.ProcurementLogo className="mr-2" />Bedarfsermittlung
          </span>
        </nav>
        <div className="App-body p-3 m-2 border rounded">
          <RequestForm />
        </div>
      </div>
    )
  }
}

export default App
