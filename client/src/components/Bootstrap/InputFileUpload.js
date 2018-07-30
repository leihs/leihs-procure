import React from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import cx from 'classnames'

import {
  endpointURL,
  fetchOptions,
  buildAuthHeaders
} from '../../apollo-client'

import { FilePicker } from './Bootstrap'

class InputFileUpload extends React.Component {
  state = { uploads: [] }

  onSelectFiles(e) {
    e.preventDefault()
    const files = [...e.target.files]
    files.forEach(rawFile => {
      const file = FileProps(rawFile)
      this.setState(
        cs => ({ uploads: [...cs.uploads, file] }),
        () =>
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
    const callback = this.props.onChange
    this.setState(
      cs => ({
        uploads: cs.uploads.map(u => (u.key === file.key ? file : u))
      }),
      () =>
        callback({
          target: { name: this.props.name, value: this.state.uploads }
        })
    )
  }

  render(
    { state, props: { id, name, className, inputProps, ...props } } = this
  ) {
    if (!id) {
      throw new Error('`InputFileUpload` is missing `props.id`!')
    }
    return (
      <div className={cx('input-group input-file-upload', className)}>
        <FilePicker
          id={id}
          name={name}
          multiple
          label="Anhänge auswählen"
          onChange={e => this.onSelectFiles(e)}
        />

        <ul className="input-file-upload-list pl-4">
          {f
            .sortBy(state.uploads, 'order')
            .map((i, n) => <li key={n}>file #{n}</li>)}
        </ul>
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

const FileProps = file => ({
  ...f.pick(file, ['name', 'size', 'type', 'lastModifiedDate']),
  key: CacheKeyFromFile(file),
  file
})

// NOTE: based on & adapted from: <https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications#Handling_the_upload_process_for_a_file>
function uploadFile({ file, onProgress, onFinish }) {
  const xhr = new XMLHttpRequest()
  const formData = new FormData()

  // headers and body:
  const headers = { Accept: 'application/json', ...buildAuthHeaders() }
  formData.append('files', file.file)

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
