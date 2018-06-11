import React from 'react'

import { Container, Main, Row, Col } from './Bootstrap'

export const MainWithSidebar = ({ sidebar, children }) => (
  // NOTE: full-page height wrapper cant also be a Row,
  //       it interferes with flexbox…
  <div style={{ minHeight: '100vh' }}>
    <Row cls="no-gutters">
      <Col className="col-md-3 col-lg-2">{sidebar}</Col>

      <Main role="main" className="col-md-9 col-lg-10 pt-3">
        <Container fluid cls="h-100">
          {children}
        </Container>
      </Main>
    </Row>
  </div>
)
