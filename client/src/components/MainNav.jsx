import React from 'react'
import f from 'lodash'
import cx from 'classnames'

import {
  NavbarBrand,
  NavItemLink,
  NavItemAnchor,
  UncontrolledDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem,
  DropdownItemLink,
  Routed
} from './Bootstrap'

import Navbar from './navbar/Navbar'
import Icon from './Icons'

function MainNav({ me, contactUrl, isDev }) {
  const sharedNavbarProps = f.try(() => JSON.parse(me.navbarProps))

  const brand = (
    <NavbarBrand key="brand" to="/">
      <Icon.LeihsProcurement className="mr-2" />
      Bedarfsermittlung
    </NavbarBrand>
  )

  return (
    <Navbar
      {...sharedNavbarProps}
      bgColor={'#343a40'} // bootstrap bg-dark
      brand={brand}
    >
      <>
        {!f.isEmpty(me) && (
          <>
            <NavItemLink href="/requests">
              <Icon.Requests fixedWidth spaced /> Anträge
            </NavItemLink>

            {me.roles.isAdmin && (
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
            )}

            {me.roles.isInspector && (
              <NavItemLink exact to="/templates/edit">
                <Icon.Templates fixedWidth spaced /> Vorlagen
              </NavItemLink>
            )}

            {!!contactUrl && (
              <NavItemAnchor href={contactUrl} target="_blank">
                <Icon.Contact fixedWidth spaced /> Kontakt
              </NavItemAnchor>
            )}
          </>
        )}

        {!!isDev && (
          <UncontrolledDropdown nav inNavbar>
            <Routed path="/dev">
              {({ isActive }) => (
                <DropdownToggle nav caret className={cx({ active: isActive })}>
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
              <DropdownItemLink to="/dev/console">API Console</DropdownItemLink>
            </DropdownMenu>
          </UncontrolledDropdown>
        )}
      </>
    </Navbar>
  )
}

export default MainNav
