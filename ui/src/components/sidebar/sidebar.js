import { useState, useRef, useEffect } from 'react'
import s from './sidebar.module.scss'
import cx from 'classnames'
import { faChevronDown, faChevronRight } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { SidebarProvider, useSidebar } from './context'

function Sidebar({ children, className }) {
  const [open, setOpen] = useState(false)
  const [hasItems, setHasItems] = useState(true)
  const ref = useRef(null)

  function handleClick() {
    return () => {
      setOpen(prev => !prev)
    }
  }

  useEffect(() => {
    if (!children || !ref.current) return

    const items = ref.current.querySelectorAll("[class^='item']")
    setHasItems(items.length)
  }, [children, ref])

  useEffect(() => {
    function closeMenu(event) {
      const target = event.target
      if (ref.current && ref.current !== target && !ref.current.contains(target)) {
        setOpen(false)
      }
    }

    document.addEventListener('click', closeMenu)

    return () => {
      document.removeEventListener('click', closeMenu)
    }
  }, [])

  if (!children || !hasItems) return null

  return (
    <SidebarProvider>
      <nav ref={ref} role="navigation" className={cx(s['sidebar'], open && s['open'], className)}>
        <ul className={cx(s['list'])}>{children}</ul>
        <button className={cx(s['open-menu'])} type="button" onClick={handleClick()}>
          <FontAwesomeIcon icon={faChevronRight} className={cx(s['arrow'], open && s['open'])} />
        </button>
      </nav>
    </SidebarProvider>
  )
}

function Section({ title, children, className }) {
  const listRef = useRef(null)
  const [hasItems, setHasItems] = useState(true)

  useEffect(() => {
    if (!children || !listRef.current) return

    const items = listRef.current.querySelectorAll("[class^='item']")

    setHasItems(items.length)
  }, [children, listRef, hasItems])

  // return nothing when children are empty
  if (!hasItems || !children) return null

  return (
    <li>
      <h1 role="heading" className={cx(s['section-title'], className)}>
        {title}
      </h1>
      <ul ref={listRef} role="list" className={cx(s.section, s['list'], className)}>
        {children}
      </ul>
    </li>
  )
}

function Item({ icon = null, href = null, active = false, children, className }) {
  const { setActive } = useSidebar()

  useEffect(() => {
    if (active && href) {
      setActive(href)
    }
  }, [])

  return (
    <li role="listitem" className={cx(s['item'], className)}>
      <a
        draggable="false"
        data-active={active}
        href={href}
        role="link"
        className={cx(s['link'], active && s['active'], className)}
      >
        {icon && <FontAwesomeIcon icon={icon} className={cx(s['icon'])} />}
        {children}
      </a>
    </li>
  )
}

function Group({ icon = null, title = '', children, className }) {
  const [open, setOpen] = useState(false)
  const [hasItems, setHasItems] = useState(true)
  const ref = useRef(null)
  const id = crypto.randomUUID()
  const { active } = useSidebar()

  function handleOpen() {
    return () => {
      setOpen(prev => !prev)
    }
  }

  useEffect(() => {
    if (active) {
      const hasActiveItem = !!ref.current?.querySelector(`a[href="${active}"]`)
      if (hasActiveItem) setOpen(true)
    }
  }, [active])

  useEffect(() => {
    if (!children || !ref.current) return

    const items = ref.current.querySelectorAll("[class^='item']")
    setHasItems(items.length)
  }, [children, ref])

  if (!children || !hasItems) return null

  return (
    <li className={cx(s['group'], s['item'], className)}>
      <button
        aria-haspopup="menu"
        aria-expanded={open ? 'true' : 'false'}
        aria-controls={id}
        type="button"
        onClick={handleOpen()}
        className={cx(s['group-button'])}
      >
        {icon && <FontAwesomeIcon icon={icon} className={cx(s['icon'])} />}
        {title}
        <FontAwesomeIcon icon={faChevronDown} className={cx(s['arrow'], open && s['open'])} />
      </button>
      <ul ref={ref} id={id} className={cx(s['group-items'], s['list'], open && s['open'], className || '')}>
        {children}
      </ul>
    </li>
  )
}

Sidebar.Item = Item
Sidebar.Section = Section
Sidebar.Group = Group

export default Sidebar
