import React, { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'

function Homepage() {
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    navigate('/requests')
  })
}

export default Homepage
