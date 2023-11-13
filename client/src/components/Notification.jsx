import React, { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { Toast, ToastHeader, ToastBody } from 'reactstrap/lib'

function Notification() {
  const { state } = useLocation()
  const notificationRef = React.useRef(null)
  const [navigationState, setNavigationState] = React.useState(null)
  const [open, setOpen] = React.useState(false)

  useEffect(() => {
    if (state?.flash) {
      setOpen(true)
      setNavigationState(state.flash)
    } else {
      setNavigationState(null)
    }
  }, [state])

  useEffect(() => {
    if (notificationRef !== null && notificationRef.current !== null) {
      notificationRef.current.classList.remove('slide-out')
      notificationRef.current.classList.add('slide-in')
      setTimeout(() => {
        // early return when there had been a route change before the timeout
        if (!notificationRef.current) return

        notificationRef.current.classList.remove('slide-in')
        notificationRef.current.classList.add('slide-out')
      }, 5000)
    }
  }, [navigationState])

  return (
    navigationState && (
      <div ref={notificationRef} className="slide-wrapper">
        <Toast isOpen={open} className="notification">
          <ToastHeader
            icon={navigationState.level}
            toggle={() => setOpen(false)}
          ></ToastHeader>
          <ToastBody>{navigationState.message}</ToastBody>
        </Toast>
      </div>
    )
  )
}

export default Notification
