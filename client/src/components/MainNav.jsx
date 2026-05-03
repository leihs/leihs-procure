import React from 'react'
import cx from 'classnames'

import Navbar from './navbar/Navbar'
import Icon from './Icons'
import { useLocation } from 'react-router-dom'
import { Link, NavLink as RouterNavLink } from 'react-router-dom'

import {
  NavbarBrand,
  UncontrolledDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem,
  NavItem,
  NavLink
} from 'reactstrap'

import t from '../locale/translate'

function MainNav({ me, contactUrl, isDev }) {
  const { pathname } = useLocation()

  const sharedNavbarProps = (() => {
    if (!me) return
    try {
      return JSON.parse(me.navbarProps)
    } catch (e) {
      console.warn('Could not parse navbarProps', e)
    }
  })()

  const brand = (
    <NavbarBrand href="/">
      <Icon.LeihsProcurement className="mr-2" />
      {t('app_title')}
    </NavbarBrand>
  )

  return (
    <Navbar
      {...sharedNavbarProps}
      bgColor={'#343a40'} // bootstrap bg-dark
      brand={brand}
    >
      <>
        {Object.keys(me).length > 0 && (
          <>
            <NavItem>
              <RouterNavLink to="/requests" className="nav-link">
                <Icon.Requests fixedWidth spaced /> {t('dashboard.requests_title_plural')}
              </RouterNavLink>
            </NavItem>

            {me.roles.isAdmin && (
              <UncontrolledDropdown nav inNavbar>
                <DropdownToggle
                  nav
                  caret
                  className={cx({ active: pathname.match('admin') })}
                >
                  <Icon.Settings /> {t('nav.admin')}
                </DropdownToggle>

                <DropdownMenu>
                  <DropdownItem
                    tag="a"
                    href="/procure/admin/budget-periods"
                    className="pl-3 text-decoration-none"
                  >
                    <Icon.BudgetPeriod fixedWidth spaced /> {t('dashboard.filter_titles.budget_periods')}
                  </DropdownItem>

                  <DropdownItem
                    tag="a"
                    href="/procure/admin/categories"
                    className="pl-3 text-decoration-none"
                  >
                    <Icon.Categories fixedWidth spaced /> {t('dashboard.filter_titles.categories')}
                  </DropdownItem>

                  <DropdownItem
                    tag="a"
                    href="/procure/admin/users"
                    className="pl-3 text-decoration-none"
                  >
                    <Icon.Users fixedWidth spaced /> {t('admin.users.title')}
                  </DropdownItem>

                  <DropdownItem
                    tag="a"
                    href="/procure/admin/organizations"
                    className="pl-3 text-decoration-none"
                  >
                    <Icon.Organizations fixedWidth spaced /> {t('dashboard.filter_titles.orgs')}
                  </DropdownItem>

                  <DropdownItem divider />

                  <DropdownItem
                    tag="a"
                    href="/procure/admin/settings"
                    className="pl-3 text-decoration-none"
                  >
                    <Icon.Settings fixedWidth spaced /> {t('nav.settings')}
                  </DropdownItem>
                </DropdownMenu>
              </UncontrolledDropdown>
            )}

            {me.roles.isInspector && (
              <NavItem>
                <Link to="/templates/edit" className="nav-link">
                  <Icon.Templates fixedWidth spaced /> {t('templates.title')}
                </Link>
              </NavItem>
            )}

            {!!contactUrl && (
              <NavLink href={contactUrl} target="_blank">
                <Icon.Contact fixedWidth spaced /> {t('nav.contact')}
              </NavLink>
            )}
          </>
        )}

        {!!isDev && (
          <UncontrolledDropdown nav inNavbar>
            <DropdownToggle
              nav
              caret
              className={cx({ active: pathname.match('dev') })}
            >
              <samp>
                <i>dev</i>
              </samp>
            </DropdownToggle>
            <DropdownMenu>
              <Link to="/dev/playground" className="text-decoration-none">
                <DropdownItem to="/dev/playground">UI Catalog</DropdownItem>
              </Link>
              <Link to="/dev/console" className="text-decoration-none">
                <DropdownItem to="/dev/console">API Console</DropdownItem>
              </Link>
            </DropdownMenu>
          </UncontrolledDropdown>
        )}
      </>
    </Navbar>
  )
}

export default MainNav
