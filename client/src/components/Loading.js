import React from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'
import Icon from './Icons'

const defaultProps = { size: '6' }
const propTypes = { size: PropTypes.oneOf(['1', '2', '3', '4', '5', '6']) }

const Loading = ({ size, cls, className }) => (
  <div className=" w-100 h-100 p-3 mx-auto d-flex flex-column">
    <div className="mb-auto" />
    <div
      className={cx(
        'w-100 p-3 text-center text-muted',
        `h${size}`,
        cls,
        className
      )}
    >
      <Icon.Spinner /> Loading...
    </div>
    <div className="mt-auto" />
  </div>
)

Loading.defaultProps = defaultProps
Loading.propTypes = propTypes
export default Loading
