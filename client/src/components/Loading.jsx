import React from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'
import Icon from './Icons'
import t from '../locale/translate'

const defaultProps = { size: '6', title: t('ui.loading_title') }
const propTypes = { size: PropTypes.oneOf(['1', '2', '3', '4', '5', '6']) }

const Loading = ({ title, size, cls, className }) => (
  <div
    className="w-100 h-100 p-3 mx-auto d-flex flex-column"
    style={{ maxHeight: '100vh' }}
  >
    <div className="mb-auto" />
    <div
      className={cx(
        'w-100 p-3 text-center text-muted',
        `h${size}`,
        cls,
        className
      )}
    >
      <Icon.Spinner /> {title}
    </div>
    <div className="mt-auto" />
  </div>
)

Loading.defaultProps = defaultProps
Loading.propTypes = propTypes
export default Loading
