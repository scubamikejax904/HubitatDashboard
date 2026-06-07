import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { initDarkMode } from './utils/darkMode'
import { syncDashboardState } from './utils/loadDashboardState'

initDarkMode()

// Fetch initial device state before render
async function bootstrap() {
  await syncDashboardState()

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
}

bootstrap()
