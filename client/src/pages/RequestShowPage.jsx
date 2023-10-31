import React from 'react'
// import f from 'lodash'

import RequestEdit from '../containers/RequestEdit'
import { useParams } from 'react-router-dom'

// # PAGE
//
const RequestShowPage = () => {
  const params = useParams()
  return (
    <div className="p-3" style={{ maxWidth: '100rem', margin: '0 auto' }}>
      <h1>
        Antrag{' '}
        {/* <IdentifierDecorator id={match.params.id} className="text-muted" /> */}
      </h1>
      {/* <RequestEdit requestShortId={match.params.id} withHeader /> */}
      <RequestEdit requestId={params.id} withHeader />
    </div>
  )
}

export default RequestShowPage
