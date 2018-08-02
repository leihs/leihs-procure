import React, { Component } from 'react'
import { withRouter } from 'react-router'

import MainNav from './MainNav'

const MainNavWithRouter = withRouter(MainNav)

class App extends Component {
  render({ props: { children, isDev } } = this) {
    return (
      // TODO: set lang to instance default language
      <div className="ui-app" lang="de">
        <MainNavWithRouter isDev={isDev} />
        <div className="minh-100vh">{children}</div>
      </div>
    )
  }
}

export default App
