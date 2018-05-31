import React from 'react'
import PropTypes from 'prop-types'

const ImageThumbnail = ({
  imageUrl,
  size = 3,
  doPlaceholder = true,
  ...imgProps
}) => (
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
      <img className="img-thumbnail" src={imageUrl} {...imgProps} alt="" />
    ) : (
      doPlaceholder && (
        <div className="img-thumbnail" style={{ height: '100%' }}>
          <div className="rounded bg-light" style={{ height: '100%' }} />
        </div>
      )
    )}
  </div>
)

ImageThumbnail.propTypes = {
  imageUrl: PropTypes.string,
  placeholder: PropTypes.bool
}

export default ImageThumbnail
