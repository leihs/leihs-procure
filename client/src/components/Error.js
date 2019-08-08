import React from 'react'
import f from 'lodash'
import { DateTime } from 'luxon'
import BrowserInfo from 'browser-info'
import t from '../locale/translate'

const browserLine = BrowserLine()

export const FatalErrorScreen = ({ error, children, title }) => {
  title = f.presence(title) || t('errors.error_title')
  return (
    <div
      style={{
        background: 'blue',
        padding: '1rem',
        height: 'calc(100vh - 3rem)',
        width: '100%',
        boxSizing: 'border-box',
        display: 'table',
        tableLayout: 'fixed'
      }}
    >
      <div
        style={{
          background: 'white',
          textAlign: 'center',
          padding: '2rem',
          display: 'table-cell',
          verticalAlign: 'middle'
        }}
      >
        {!!title && <h1 className="h4">{title}</h1>}
        <div>{!!children && children}</div>
        {!!error && (
          <p>
            <JsonDetails
              error={{
                error,
                env: {
                  target: process.env.NODE_ENV,
                  browser: browserLine,
                  time: (d => [new DateTime(d).toISO(), d.toJSON()])(new Date())
                }
              }}
            />
          </p>
        )}
      </div>
    </div>
  )
}

export const ErrorPanel = ({ error, data, errorDetails }) => {
  const apiErrors = f.presence(f.get(error, 'graphQLErrors'))
  const networkError = f.presence(f.get(error, 'networkError'))
  errorDetails = f.presence(errorDetails)

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

      {errorDetails && (
        <JsonDetails title={t('errors.error_details')} error={errorDetails} />
      )}

      {networkError && (
        <JsonDetails title={t('errors.network_error')} error={networkError} />
      )}

      {apiErrors && (
        <JsonDetails title={t('errors.api_error_details')} error={apiErrors} />
      )}

      {!f.isEmpty(data) && (
        <JsonDetails title={t('errors.data_details')} error={data} />
      )}

      <p className="text-muted mb-0">
        <small>
          <samp>
            {new Date().toJSON()}
            {'  '}
          </samp>
          <span>{t('errors.error_panel_emoticon')}</span>
          <samp>
            {'  '}
            {browserLine}
          </samp>
        </small>
      </p>
    </div>
  )
}

export function getErrorCode(error) {
  const code = f.first(f.map(f.get(error, 'graphQLErrors'), 'extensions.code'))
  if (code) return code

  if (f.get(error, 'networkError.message') === 'Failed to fetch') {
    return 'NO_CONNECTION_TO_SERVER'
  }

  if (f.get(error, 'networkError')) {
    return 'UNKNOWN_NETWORK_ERROR'
  }
}

const JsonDetails = ({ error, title = t('errors.show_details') }) =>
  !!error && (
    <details>
      <summary>
        <small>{title}</small>
      </summary>
      <pre style={{ textAlign: 'left', whiteSpace: 'pre-wrap' }}>
        <small>
          {f.try(() => JSON.stringify(error, 0, 2)) || String(error)}
        </small>
      </pre>
    </details>
  )

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
