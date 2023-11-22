import { createContext, useContext, useState } from 'react'

export const SidebarContext = createContext(null)

export function SidebarProvider({ children }) {
  const [active, setActive] = useState()
  return <SidebarContext.Provider value={{ active, setActive }}>{children}</SidebarContext.Provider>
}

export function useSidebar() {
  return useContext(SidebarContext)
}
