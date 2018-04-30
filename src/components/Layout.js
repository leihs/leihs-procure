import React from 'react'

import { Container, Div, Main, Row, Col } from './Bootstrap'

export const MainWithSidebar = ({ sidebar, children }) => (
  <Div>
    <Row cls="no-gutters">
      <Col md="2">{sidebar}</Col>

      <Main role="main" className="col-md-9 ml-sm-auto col-lg-10 pt-3 px-4">
        <Container fluid>{children}</Container>
      </Main>
    </Row>
  </Div>
)
