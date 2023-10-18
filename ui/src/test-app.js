import ReactDOM from 'react-dom'
import React, { useState } from 'react'
import './styles/styles.scss'
import Navbar from './components/navbar/Navbar'
import sampleNavbarProps from './components/navbar/sampleProps.json'
import DatePicker from './components/DatePicker'
import DemoComponent from './components/DemoComponent'

// React 18: import ReactDOM from 'react-dom/client';

function App() {
  const [selectedDate, setSelectedDate] = useState()
  const onChange = event => {
    setSelectedDate(event.target.value)
  }

  return (
    <>
      <div className="container mt-5 p-5 border rounded">
        <h1>Leihs Admin UI Test App</h1>

        <h2>Components</h2>

        <h3>Navbar</h3>
        <div className="mb-4">
          <div>Check `my` app for detailed navbar stories</div>
          <Navbar {...sampleNavbarProps} />
        </div>

        <h3>Calendar</h3>
        <div className="mb-4">
          <DatePicker
            required
            value={selectedDate}
            onChange={onChange}
            className="m-auto"
            displayMode="date"
            showPreview={false}
            months={1}
            minDate="now"
          />
        </div>

        <h3>DemoComponent</h3>
        <div className="mb-4">
          <div>Trivial component for debugging</div>
          <div>
            <DemoComponent />
          </div>
        </div>

        <h2>Theme</h2>
        <button className="btn btn-primary mr-2">Primary button</button>
        <button className="btn btn-secondary">Primary button</button>
      </div>
    </>
  )
}

const container = document.getElementById('root')

ReactDOM.render(<App />, container)

// React 18:
// const root = ReactDOM.createRoot(container);
// root.render(<App />);
