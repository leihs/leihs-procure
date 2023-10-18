import React, { useState } from 'react'

export default function DemoComponent() {
  const [count, setCount] = useState(0)
  return (
    <div>
      <button className="btn btn-outline-primary" onClick={() => setCount(x => x + 1)}>
        Click me {count}
      </button>
    </div>
  )
}
