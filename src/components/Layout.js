import React from 'react'

import { Row, Col } from './Bootstrap'

export const MainWithSidebar = ({ sidebar, children }) => (
  <div className="container-fluid">
    <Row>
      <Col md="2" cls="bg-light">
        {sidebar}
      </Col>

      <main role="main" className="col-md-9 ml-sm-auto col-lg-10 pt-3 px-4">
        {children}
      </main>
    </Row>
  </div>
)
