import React, { Component } from 'react'

import GraphiQL from 'graphiql'
import 'graphiql/graphiql.css'
import 'codemirror/theme/dracula.css'

import {
  endpointURL,
  fetchOptions,
  buildAuthHeaders
} from '../../apollo-client'

const exampleQuery = `
query allTheRooms {
  rooms {
    id
    name
    building {
      name
    }
  }
}
`.trim()

function graphQLFetcher(graphQLParams) {
  return fetch(endpointURL, {
    ...fetchOptions,
    headers: { 'Content-Type': 'application/json', ...buildAuthHeaders() },
    method: 'post',
    body: JSON.stringify(graphQLParams)
  }).then(response => response.json())
}

class DevConsole extends Component {
  render() {
    return (
      <div style={{ height: '100vh' }}>
        <GraphiQL
          fetcher={graphQLFetcher}
          query={exampleQuery}
          editorTheme="dracula"
        >
          <GraphiQL.Logo>
            <b>API Console</b>
          </GraphiQL.Logo>
        </GraphiQL>
      </div>
    )
  }
}

export default DevConsole
