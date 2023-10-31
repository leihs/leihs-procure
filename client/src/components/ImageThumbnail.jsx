import React from 'react'
import PropTypes from 'prop-types'
import cx from 'classnames'

const ImageThumbnail = ({
  imageUrl,
  size = 3,
  doPlaceholder = true,
  framed = false,
  className,
  ...imgProps
}) => {
  const imgCls = cx(
    'img-thumbnail',
    { 'bg-transparent border-0': !framed },
    className
  )
  return (
    <div
      className="img-thumbnail-wrapper text-center mr-2"
      style={{
        height: `${size}rem`,
        width: `${size}rem`,
        display: 'inline-block',
        verticalAlign: 'middle'
      }}
    >
      {imageUrl ? (
        <img src={imageUrl} alt="" className={imgCls} {...imgProps} />
      ) : (
        doPlaceholder && (
          <div className={imgCls} style={{ height: '100%' }}>
            <div className="rounded bg-light" style={{ height: '100%' }} />
          </div>
        )
      )}
    </div>
  )
}

ImageThumbnail.propTypes = {
  imageUrl: PropTypes.string,
  placeholder: PropTypes.bool
}

export default ImageThumbnail
