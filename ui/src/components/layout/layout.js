import cx from 'classnames'
import s from './layout.module.scss'

function Header({ children, className }) {
  return <header className={cx(s['header'], className)}>{children}</header>
}

function Footer({ children, className }) {
  return <footer className={cx(s['footer'], className)}>{children}</footer>
}

function Main({ children, className }) {
  return <main className={cx(s['main'], className)}>{children}</main>
}

function Aside({ children, className }) {
  return <aside className={cx(s['aside'], className)}>{children}</aside>
}

function Layout({ children, className }) {
  return <div className={cx(s['layout'], className)}>{children}</div>
}

Layout.Header = Header
Layout.Footer = Footer
Layout.Main = Main
Layout.Aside = Aside

export default Layout
