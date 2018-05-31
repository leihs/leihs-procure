import React from 'react'

import { Container, Main, Row, Col } from './Bootstrap'

export const MainWithSidebar = ({ sidebar, children }) => (
  <Row cls="no-gutters" style={{ minHeight: '100vh' }}>
    <Col className="col-md-3 col-lg-2">{sidebar}</Col>

    <Main role="main" className="col-md-9 col-lg-10 pt-3">
      <Container fluid>{children}</Container>
    </Main>
  </Row>
)
