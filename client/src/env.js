export const isDev =
  process.env.NODE_ENV !== 'production' && process.env.NODE_ENV !== 'test'

export const store = window.sessionStorage

export const supportsHistory = 'pushState' in window.history
