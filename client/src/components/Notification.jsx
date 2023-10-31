import React, { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

import { Toast, ToastHeader, ToastBody } from 'reactstrap/lib'

function Notification() {
  const { state } = useLocation()
  const notificationRef = React.useRef(null)
  const [navigationState, setNavigationState] = React.useState(null)

  useEffect(() => {
    if (state?.flash) {
      setNavigationState(state.flash)
    } else {
      setNavigationState(null)
    }
  }, [state])

  useEffect(() => {
    if (notificationRef && notificationRef.current) {
      notificationRef.current.classList.remove('slide-out')
      notificationRef.current.classList.add('slide-in')
      setTimeout(() => {
        notificationRef.current.classList.remove('slide-in')
        notificationRef.current.classList.add('slide-out')
      }, 5000)
    }
  }, [navigationState])

  return (
    navigationState && (
      <div ref={notificationRef} className="slide-wrapper">
        <Toast className="notification">
          <ToastHeader icon={navigationState.level}></ToastHeader>
          <ToastBody>{navigationState.message}</ToastBody>
        </Toast>
      </div>
    )
  )
}

export default Notification
