import React, { Fragment as F } from 'react'
import cx from 'classnames'

import {
  Collapse,
  Navbar,
  NavbarToggler,
  NavbarBrand,
  Nav,
  NavItemLink,
  UncontrolledDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem,
  DropdownItemLink,
  Routed
} from './Bootstrap'

import Icon from './Icons'

const TITLE = 'Bedarfsermittlung'

const Brand = () => (
  <F>
    <Icon.LeihsProcurement className="mr-2" />
    {TITLE}
  </F>
)

export default class MainNav extends React.Component {
  state = {
    isOpen: false
  }
  toggleOpen() {
    this.setState({
      isOpen: !this.state.isOpen
    })
  }
  render({ props: { isDev }, state } = this) {
    return (
      <div>
        <Navbar dark color="dark" expand="lg">
          <NavbarBrand exact to="/requests">
            <Brand />
          </NavbarBrand>

          <NavbarToggler onClick={e => this.toggleOpen()} />

          <Collapse isOpen={state.isOpen} navbar>
            <Nav className="mr-auto" navbar>
              <NavItemLink exact to="/requests">
                <Icon.Requests fixedWidth spaced /> Anträge
              </NavItemLink>

              <UncontrolledDropdown nav inNavbar>
                <Routed path="/admin">
                  {({ isActive }) => (
                    <DropdownToggle
                      nav
                      caret
                      className={cx({ active: isActive })}
                    >
                      <Icon.Settings /> Admin
                    </DropdownToggle>
                  )}
                </Routed>

                <DropdownMenu right>
                  <DropdownItemLink className="pl-3" to="/admin/budget-periods">
                    <Icon.BudgetPeriod fixedWidth spaced /> Budgetperioden
                  </DropdownItemLink>

                  <DropdownItemLink className="pl-3" to="/admin/categories">
                    <Icon.Categories fixedWidth spaced /> Kategorien
                  </DropdownItemLink>

                  <DropdownItemLink className="pl-3" to="/admin/users">
                    <Icon.Users fixedWidth spaced /> Benutzer
                  </DropdownItemLink>

                  <DropdownItemLink className="pl-3" to="/admin/organizations">
                    <Icon.Organizations fixedWidth spaced /> Organisationen
                  </DropdownItemLink>

                  <DropdownItem divider />

                  <DropdownItemLink className="pl-3" to="/admin/settings">
                    <Icon.Settings fixedWidth spaced /> Einstellungen
                  </DropdownItemLink>
                </DropdownMenu>
              </UncontrolledDropdown>

              <NavItemLink exact to="/TODO">
                <Icon.Contact fixedWidth spaced /> Kontakt
              </NavItemLink>

              {!!isDev && (
                <UncontrolledDropdown nav inNavbar>
                  <Routed path="/dev">
                    {({ isActive }) => (
                      <DropdownToggle
                        nav
                        caret
                        className={cx({ active: isActive })}
                      >
                        <samp>
                          <i>dev</i>
                        </samp>
                      </DropdownToggle>
                    )}
                  </Routed>
                  <DropdownMenu right>
                    <DropdownItemLink to="/dev/playground">
                      UI Catalog
                    </DropdownItemLink>
                    <DropdownItemLink to="/dev/console">
                      API Console
                    </DropdownItemLink>
                  </DropdownMenu>
                </UncontrolledDropdown>
              )}
            </Nav>

            <Nav className="ml-auto" navbar>
              <UncontrolledDropdown nav inNavbar>
                <DropdownToggle nav caret>
                  <Icon.LeihsProcurement /> Bedarfsermittlung
                </DropdownToggle>
                <DropdownMenu right>
                  <DropdownItem>
                    <Icon.LeihsBorrow /> Ausleihen
                  </DropdownItem>
                  <DropdownItem divider />
                  <DropdownItem>
                    <Icon.LeihsAdmin /> Admin
                  </DropdownItem>
                  <DropdownItem divider />
                  <DropdownItemLink to="/">
                    <Icon.LeihsProcurement /> Bedarfsermittlung
                  </DropdownItemLink>
                  <DropdownItem divider />
                  <DropdownItem>
                    <Icon.LeihsManage />
                  </DropdownItem>
                </DropdownMenu>
              </UncontrolledDropdown>

              <UncontrolledDropdown nav inNavbar>
                <DropdownToggle nav caret>
                  User
                </DropdownToggle>
                <DropdownMenu right>
                  <DropdownItem>[TODO]</DropdownItem>
                </DropdownMenu>
              </UncontrolledDropdown>

              <UncontrolledDropdown nav inNavbar>
                <DropdownToggle nav caret>
                  <Icon.Language />
                </DropdownToggle>
                <DropdownMenu right>
                  <DropdownItem>[TODO]</DropdownItem>
                </DropdownMenu>
              </UncontrolledDropdown>
            </Nav>
          </Collapse>
        </Navbar>
      </div>
    )
  }
}
