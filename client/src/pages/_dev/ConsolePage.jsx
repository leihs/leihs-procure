import React, { Component } from 'react'

import GraphiQL from 'graphiql'
import 'graphiql/graphiql.css'
import 'codemirror/theme/dracula.css'

import {
  endpointURL,
  fetchOptions,
  defaultHeaders,
  buildAuthHeaders
} from '../../apollo-client'

const exampleQuery = `
query meAndMyRequests {
  current_user {
    user {
      email
    }
  }
  requests(requested_by_auth_user: true) {
    id
    budget_period {
      value {
        name
      }
    }
    model {
      value {
        id
        product 
      }
    }
    article_name {
      value
    }
    motivation {
      value
    }
  }
}
`.trim()

function graphQLFetcher(graphQLParams) {
  return fetch(endpointURL, {
    ...fetchOptions,
    headers: {
      'Content-Type': 'application/json',
      ...defaultHeaders,
      ...buildAuthHeaders()
    },
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
          defaultQuery={exampleQuery}
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
