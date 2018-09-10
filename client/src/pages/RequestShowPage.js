import React from 'react'
// import f from 'lodash'

// import StatefulForm from '../components/StatefulForm'
// import Icon from '../components/Icons'
import { RouteParams as Routed } from '../components/Bootstrap'
// import Loading from '../components/Loading'

import RequestEdit from '../containers/RequestEdit'

// # PAGE
//
const RequestShowPage = () => (
  <Routed>
    {({ match }) => (
      <div className="p-3" style={{ maxWidth: '100rem', margin: '0 auto' }}>
        <h1>Antrag</h1>
        <RequestEdit requestId={match.params.id} withHeader />
      </div>
    )}
  </Routed>
)

export default RequestShowPage
