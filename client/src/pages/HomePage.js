import React from 'react'
// import f from 'lodash'
import { NavLink } from 'react-router-dom'

import { ListGroup, ListGroupItem } from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'

const ListGroupLink = p => <ListGroupItem {...p} tag={NavLink} />

const Homepage = p => (
  <MainWithSidebar>
    <ListGroup className="mt-5">
      <ListGroupLink to="/requests/new">Neuen Antrag erstellen</ListGroupLink>
      {/* TODO: link to 'my requests' (needs param handling on RequestsIndex) 
        <ListGroupLink to='/requests'>Meine Anträge ansehen</ListGroupLink>
      */}
      <ListGroupLink to="/requests">Alle Anträge ansehen</ListGroupLink>
    </ListGroup>
  </MainWithSidebar>
)

export default Homepage
