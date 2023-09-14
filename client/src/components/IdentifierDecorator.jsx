import React from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'

const SOFT_BREAK = '​'

const defaultProps = { separator: '.', size: '6' }
const propTypes = {
  id: PropTypes.string.isRequired,
  separator: PropTypes.string
}

// Force "nice" linebreaks at separating characters, but ignore them for selections,
// so the original string will be in the clipboard when copying.
const IdentifierDecorator = ({ id, separator, cls, className, ...rest }) =>
  !!id && (
    <samp className={cx(cls, className)} {...rest}>
      {id.split(separator).map((str, i, a) => (
        <React.Fragment key={i}>
          {/* <span>{str}</span> */}
          {str}
          {i < a.length - 1 && separator}
          <span style={{ userSelect: 'none' }}>{SOFT_BREAK}</span>
        </React.Fragment>
      ))}
    </samp>
  )

IdentifierDecorator.defaultProps = defaultProps
IdentifierDecorator.propTypes = propTypes
export default IdentifierDecorator
