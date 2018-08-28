import React, { Fragment as F } from 'react'
import f from 'lodash'
import cx from 'classnames'

import {
  Badge,
  Collapse,
  Navbar,
  NavbarToggler,
  NavbarBrand,
  Nav,
  NavItemLink,
  NavItemAnchor,
  UncontrolledDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem,
  DropdownItemLink,
  Routed
} from './Bootstrap'

import Icon from './Icons'
import { DisplayName } from './decorators'

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
  render({ props: { me, contactUrl, isDev }, state } = this) {
    return (
      <div>
        <Navbar dark color="dark" expand="lg">
          <NavbarBrand exact to="/">
            <Brand />
          </NavbarBrand>

          <NavbarToggler onClick={e => this.toggleOpen()} />

          <Collapse isOpen={state.isOpen} navbar>
            <Nav className="mr-auto" navbar>
              <NavItemLink exact to="/requests">
                <Icon.Requests fixedWidth spaced /> Anträge
              </NavItemLink>

              {me.permissions.isAdmin && (
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
                    <DropdownItemLink
                      className="pl-3"
                      to="/admin/budget-periods"
                    >
                      <Icon.BudgetPeriod fixedWidth spaced /> Budgetperioden
                    </DropdownItemLink>

                    <DropdownItemLink className="pl-3" to="/admin/categories">
                      <Icon.Categories fixedWidth spaced /> Kategorien
                    </DropdownItemLink>

                    <DropdownItemLink className="pl-3" to="/admin/users">
                      <Icon.Users fixedWidth spaced /> Benutzer
                    </DropdownItemLink>

                    <DropdownItemLink
                      className="pl-3"
                      to="/admin/organizations"
                    >
                      <Icon.Organizations fixedWidth spaced /> Organisationen
                    </DropdownItemLink>

                    <DropdownItem divider />

                    <DropdownItemLink className="pl-3" to="/admin/settings">
                      <Icon.Settings fixedWidth spaced /> Einstellungen
                    </DropdownItemLink>
                  </DropdownMenu>
                </UncontrolledDropdown>
              )}

              {1 && (
                <NavItemLink exact to="/templates/edit">
                  <Icon.Templates fixedWidth spaced /> Vorlagen
                </NavItemLink>
              )}

              {!!contactUrl && (
                <NavItemAnchor href={contactUrl} target="_blank">
                  <Icon.Contact fixedWidth spaced /> Kontakt
                </NavItemAnchor>
              )}

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
                    <Icon.LeihsManage /> Manage
                  </DropdownItem>
                </DropdownMenu>
              </UncontrolledDropdown>

              <UncontrolledDropdown nav inNavbar>
                <DropdownToggle nav caret>
                  <Icon.User size="lg" /> {DisplayName(me, { abbr: true })}
                </DropdownToggle>
                <DropdownMenu right>
                  <DropdownItem>{tmpUserInfo(me)}</DropdownItem>
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

const tmpUserInfo = me => (
  <F>
    {[
      'isAdmin',
      'isRequester',
      'isInspectorForCategories',
      'isViewerForCategories'
    ]
      .map(k => [k, me.permissions[k]])
      .filter(([k, i]) => i && f.present(i))
      .map(([k, i]) => (
        <F key={k}>
          <Badge dark>
            {k.replace(/ForCategories$/, ` (${i.length} categories)`)}
          </Badge>{' '}
        </F>
      ))}
    <pre>
      <small>{JSON.stringify(f.omit(me, 'permissions'), 0, 2)}</small>
    </pre>
  </F>
)
