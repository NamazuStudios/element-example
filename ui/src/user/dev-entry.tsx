import React from 'react'
import ReactDOM from 'react-dom/client'
import { ExamplePlugin } from './ExamplePlugin'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ExamplePlugin />
  </React.StrictMode>
)
