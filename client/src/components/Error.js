import React from 'react'
import f from 'lodash'
import BrowserInfo from 'browser-info'

const browserLine = BrowserLine()

export const ErrorPanel = ({ error, data }) => {
  return (
    <div
      className="text-dark rounded border border-red py-2 px-4 m-2"
      style={{ background: 'tomato' }}
    >
      <p>
        <b>Error </b>
        <samp>:</samp>(
      </p>

      <pre>{error.toString()}</pre>

      {error &&
        !f.isEmpty(error.graphQLErrors) && (
          <details>
            <summary>Error Details:</summary>
            <pre>{JSON.stringify(error.graphQLErrors, 0, 2)}</pre>
          </details>
        )}

      {!f.isEmpty(data) && (
        <details>
          <summary>Data:</summary>
          <pre>{JSON.stringify(data, 0, 2)}</pre>
        </details>
      )}

      <p className="text-muted mb-0">
        <small>
          <samp>
            {new Date().toJSON()}
            {'  '}
          </samp>
          <span>¯\_༼ ༎ຶ ෴ ༎ຶ༽_/¯</span>
          <samp>
            {'  '}
            {browserLine}
          </samp>
        </small>
      </p>
    </div>
  )
}

function BrowserLine(browserInfo) {
  const fallback = { os: 'Unknown' }
  const env = { ...fallback, ...BrowserInfo() }
  const osName = (env.os || fallback.os)
    .replace('OS X', 'macOS')
    .replace(/^Linux/, 'GNU/Linux')

  return !(env.name && env.version)
    ? f.get(window, 'navigator.userAgent') || fallback.os
    : `${env.name} ${env.version} on ${osName}`
}
