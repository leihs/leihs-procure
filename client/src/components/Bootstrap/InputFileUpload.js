import React from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import cx from 'classnames'

import {
  endpointURL,
  fetchOptions,
  buildAuthHeaders
} from '../../apollo-client'

import { Button, Tooltipped, FilePicker } from '../Bootstrap'
import Icon from '../Icons'
import t from '../../locale/translate'

import logger from 'debug'
const log = logger('app:ui:InputFileUpload')

class InputFileUpload extends React.Component {
  constructor(props) {
    super(props)
    this.state = { uploads: props.value || [] }
    log('init', { props, state: this.state })
  }

  onSelectFiles(e) {
    const files = [...e.target.files]
    log('onSelectFiles', { files })

    // braindead async queue, recursing callbacks-based
    const addFileToQueue = index => {
      if (index >= files.length) return // end of queue
      const file = FileObj(files[index])
      let isDuplicate = false

      this.setState(
        cs => {
          if (f.find(cs.uploads, { key: file.key })) return (isDuplicate = true)
          return { uploads: [...cs.uploads, file] }
        },
        () => {
          !isDuplicate &&
            uploadFile({
              file,
              onProgress: (i, n) => this.onFileProgress(i, n),
              onFinish: i => this.onUploadedFile(i)
            })
          addFileToQueue(index + 1) // recurse
        }
      )
    }
    addFileToQueue(0, addFileToQueue)
  }

  onRetryFile(file) {
    log('onFileProgress', { file })
    this.setState(
      cs => ({
        uploads: cs.uploads.filter(u => !sameKeyOrId(u, file))
      }),
      () => {
        this.onSelectFiles({ target: { files: [file.rawFile] } })
      }
    )
  }

  onFileProgress(file) {
    log('onFileProgress', { file })
    this.setState(cs => ({
      uploads: cs.uploads.map(u => (sameKeyOrId(u, file) ? file : u))
    }))
  }

  onUploadedFile(file) {
    log('onUploadedFile', { file })
    this.setState(
      cs => ({
        uploads: cs.uploads.map(u => (sameKeyOrId(u, file) ? file : u))
      }),
      () => this.onChangeCallback()
    )
  }

  onMarkForDeletion(file, toggled) {
    log('onMarkForDeletion', { file, toDelete: toggled })
    this.setState(
      cs => ({
        uploads: cs.uploads.map(
          u =>
            (file.id
            ? u.id === file.id
            : u.key === file.key)
              ? { ...u, toDelete: toggled }
              : u
        )
      }),
      () => this.onChangeCallback()
    )
  }

  onChangeCallback() {
    const callback = this.props.onChange
    const value = this.state.uploads.map(u => ({ ...u, rawFile: undefined }))
    callback({ target: { name: this.props.name, value } })
  }

  render(
    { state, props: { id, name, className, inputProps, ...props } } = this
  ) {
    log('render', { state, props: this.props })
    if (!id) {
      throw new Error('`InputFileUpload` is missing `props.id`!')
    }
    const isDisabled = !!(props.disabled || props.readOnly)

    return (
      <div id={id} className={cx('input-group input-file-upload', className)}>
        {f.present(state.uploads) && (
          <ul className="input-file-upload-list pl-4 mb-1">
            {state.uploads.map(u => {
              const key = u.id || u.key
              const isReady = !!u.id
              const isFailed = !!u.error
              const txtCls = cx({
                'text-strike text-danger': u.toDelete,
                'text-strike text-warning': isFailed
              })
              const nameOrLink = !u.url ? (
                <span className={txtCls}>{u.filename}</span>
              ) : (
                <a
                  href={u.url}
                  className={txtCls}
                  data-download={u.filename}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {u.filename}
                </a>
              )

              const deleteToggle = isDisabled || (
                <label>
                  <Icon.Trash className="text-danger" />
                  <input
                    type="checkbox"
                    className="sr-only"
                    checked={!!u.toDelete}
                    onClick={e => this.onMarkForDeletion(u, !u.toDelete)}
                  />
                </label>
              )

              const errorMarker = isFailed && (
                <Tooltipped text={t('upload_error_click_to_show')}>
                  <Button
                    id={`error-button-${key.replace(/[^\w\d]/g, '')}`}
                    color="link"
                    size="sm"
                    onClick={e =>
                      alert(`Error Details:\n${JSON.stringify(u.error, 0, 2)}`)
                    }
                  >
                    <Icon.WarningSign className="text-warning" />
                  </Button>
                </Tooltipped>
              )

              const retryButton = isFailed && (
                <Tooltipped text={t('upload_error_click_to_retry')}>
                  <Button
                    id={`retry-button-${key.replace(/[^\w\d]/g, '')}`}
                    color="link"
                    size="sm"
                    onClick={e => this.onRetryFile(u)}
                  >
                    <Icon.Reload className="text-primary" />
                  </Button>
                </Tooltipped>
              )

              const endOfLine = isReady ? (
                deleteToggle
              ) : !isFailed ? (
                <Icon.Spinner />
              ) : (
                retryButton
              )

              return (
                <li key={key} className={txtCls}>
                  {errorMarker}{' '}
                  <span className="d-inline-block py-2">{nameOrLink}</span>{' '}
                  {endOfLine}
                </li>
              )
            })}
          </ul>
        )}

        {isDisabled || (
          <FilePicker
            // FIXME: re-enable multiples when backend fixed!
            multiple={false}
            label="Anhänge auswählen"
            onChange={e => this.onSelectFiles(e)}
          />
        )}
      </div>
    )
  }
}

InputFileUpload.defaultProps = {
  name: 'files',
  onChange: () => {},
  inputProps: {}
}

InputFileUpload.propTypes = {
  id: PropTypes.string,
  className: PropTypes.string,
  onChange: PropTypes.func
  // value: PropTypes.oneOfType([PropTypes.string])
}

export default InputFileUpload

const CacheKeyFromFile = file =>
  ['name', 'size', 'lastModified'].map(k => file[k]).join('-')

const FileObj = file => ({
  ...f.pick(file, ['size', 'type', 'lastModifiedDate']),
  key: CacheKeyFromFile(file),
  filename: file.name,
  rawFile: file,
  toDelete: false,
  typename: 'Upload'
})

// NOTE: based on & adapted from: <https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications#Handling_the_upload_process_for_a_file>
const logu = logger('app:ui:libUploadFile')
function uploadFile({ file, onProgress, onFinish }) {
  logu('init', { file })
  const xhr = new XMLHttpRequest()
  const formData = new FormData()

  // headers and body:
  const headers = { Accept: 'application/json', ...buildAuthHeaders() }
  formData.append('files', file.rawFile)

  // progress reporting:
  let progress = 0
  const onUploadProgress = e => {
    if (e.lengthComputable) {
      progress = Math.round((e.loaded * 100) / e.total)
      logu('onUploadProgress', { progress, file })
      return onProgress({ ...file, progress })
    }
    logu('onUploadProgress', 'LENGTH_NOT_COMPUTABLE!')
  }
  const onUploadFinish = e => {
    logu('onUploadFinish', { file })
    progress = 100
    onProgress({ ...file, progress })
  }

  // handle successful upload
  const onFileFinish = e => {
    let error = false
    progress = 100
    const response = f.try(() => JSON.parse(xhr.response))
    if (!response || response.length !== 1) {
      logu('onFileFinish | Upload error!', xhr)
      error = f.get(response, 'errors') || response || true
    }
    const obj = { ...file, ...f.first(response), progress, error: error }
    logu('onFileFinish', obj)
    onFinish(obj)
  }

  // attach events:
  xhr.upload.addEventListener('load', onUploadFinish, false)
  xhr.addEventListener('load', onFileFinish, false)
  xhr.upload.addEventListener(
    'progress',
    f.throttle(onUploadProgress, 3000), // dont update too often
    false
  )

  // send request:
  xhr.open('POST', endpointURL.replace('/graphql', '/upload'))
  f.map(headers, (v, k) => xhr.setRequestHeader(k, v))
  if (fetchOptions.credentials === 'omit') {
    xhr.withCredentials = false
  }
  xhr.send(formData)
}

const sameKeyOrId = (a, b) => !!(a.key && b.key && f.isEqual(a.key, b.key))
