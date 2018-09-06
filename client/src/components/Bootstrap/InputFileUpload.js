import React from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import cx from 'classnames'

import {
  endpointURL,
  fetchOptions,
  buildAuthHeaders
} from '../../apollo-client'

import { FilePicker } from '.'
import Icon from '../Icons'

class InputFileUpload extends React.Component {
  constructor(props) {
    super(props)
    this.state = { uploads: props.value || [] }
  }

  onSelectFiles(e) {
    e.preventDefault()
    const files = [...e.target.files]
    files.forEach(rawFile => {
      const file = FileObj(rawFile)
      let isDuplicate = false
      this.setState(
        cs => {
          if (f.find(cs.uploads, { key: file.key })) {
            return (isDuplicate = true)
          }
          return { uploads: [...cs.uploads, file] }
        },
        () =>
          !isDuplicate &&
          uploadFile({
            file,
            onProgress: (i, n) => this.onFileProgress(i, n),
            onFinish: i => this.onUploadedFile(i)
          })
      )
    })
  }

  onFileProgress(file) {
    this.setState(cs => ({
      uploads: cs.uploads.map(u => (u.key === file.key ? file : u))
    }))
  }

  onUploadedFile(file) {
    this.setState(
      cs => ({
        uploads: cs.uploads.map(u => (u.key === file.key ? file : u))
      }),
      () => this.onChangeCallback()
    )
  }

  onMarkForDeletion(file, toggled) {
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
    callback({
      target: { name: this.props.name, value }
    })
  }

  render(
    { state, props: { id, name, className, inputProps, ...props } } = this
  ) {
    if (!id) {
      throw new Error('`InputFileUpload` is missing `props.id`!')
    }
    const isDisabled = !!(props.disabled || props.readOnly)

    return (
      <div id={id} className={cx('input-group input-file-upload', className)}>
        {f.present(state.uploads) && (
          <ul className="input-file-upload-list pl-4 mb-1">
            {state.uploads.map(u => {
              const txtCls = cx({ 'text-strike text-danger': u.toDelete })
              const nameOrLink = !u.url ? (
                u.filename
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
              return (
                <li key={u.id || u.key} className={txtCls}>
                  {nameOrLink} {!u.id ? <Icon.Spinner /> : deleteToggle}
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
  __typename: 'Upload'
})

// NOTE: based on & adapted from: <https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications#Handling_the_upload_process_for_a_file>
function uploadFile({ file, onProgress, onFinish }) {
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
      onProgress({ ...file, progress })
    }
  }
  const onUploadFinish = e => {
    progress = 100
    onProgress({ ...file, progress })
  }

  // handle successful upload
  const onFileFinish = e => {
    const result = f.try(() => JSON.parse(xhr.response))
    if (!result || result.length !== 1) {
      return window.alert('Upload error!')
    }
    onFinish({ ...file, progress: 100, ...result[0] })
  }

  // attach events:
  xhr.upload.addEventListener('load', onUploadFinish, false)
  xhr.addEventListener('load', onFileFinish, false)
  xhr.upload.addEventListener(
    'progress',
    f.throttle(onUploadProgress, 300), // dont update too often
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
