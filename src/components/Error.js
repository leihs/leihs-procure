import React from 'react'
import f from 'lodash'
import BrowserInfo from 'browser-info'

const browserLine = BrowserLine()

export const ErrorPanel = ({ error }) => {
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
  const env = { os: 'Unknown', ...BrowserInfo() }
  const osName = env.os.replace('OS X', 'macOS').replace(/^Linux/, 'GNU/Linux')

  return !(env.name && env.version)
    ? f.get(window, 'navigator.userAgent') || 'Unknown'
    : `${env.name} ${env.version} on ${osName}`
}
