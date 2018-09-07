import React from 'react'
import cx from 'classnames'
// import f from 'lodash'

import { Row, Col, Collapsing } from '.'

export const examples = [
  {
    title: 'basic',
    content: (
      <Row>
        <Col sm>
          <Collapsing id="collapse-example-basic" startOpen>
            {({ isOpen, togglerProps, collapsedProps, Caret }) => (
              <div>
                <div {...togglerProps}>
                  <Caret spaced />
                  Collapsed Title
                </div>
                {isOpen && <div {...collapsedProps}>Collapsed Content!</div>}
              </div>
            )}
          </Collapsing>
        </Col>
        <Col sm>
          <pre>
            <code>{`
<Collapsing id="collapse-example-1" startOpen>
  {({ isOpen, togglerProps, collapsedProps, Caret }) => (
    <div>
      <div {...togglerProps}>
        <Caret spaced />
        Collapsed Title
      </div>
      {isOpen && <div {...collapsedProps}>Collapsed Content!</div>}
    </div>
  )}
</Collapsing>
        `}</code>
          </pre>
        </Col>
      </Row>
    )
  },
  {
    title: 'card style',
    content: (
      <Row>
        <Col sm>
          <Collapsing id="collapse-example-card" startOpen>
            {({ isOpen, togglerProps, collapsedProps, Caret }) => (
              <div className={cx('card mb-3')}>
                <div
                  className={cx('card-header cursor-pointer pl-2', {
                    'border-bottom-0': !isOpen
                  })}
                  {...togglerProps}
                >
                  <h1 className="mb-0 h4 d-inline-block">
                    <Caret spaced />
                    Collapsed Title
                  </h1>
                </div>

                {isOpen && (
                  <div className={cx('card-body pl-2')} {...collapsedProps}>
                    <p className="h5 p-4">Collapsed Content!</p>
                  </div>
                )}
              </div>
            )}
          </Collapsing>
        </Col>
      </Row>
    )
  }
]
