import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from './RootLayout'
import { HomePage } from './HomePage'

// /login and /register are placeholders here — Batch 4 (auth feature) replaces
// them with the real LoginPage/RegisterPage components.
export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <div className="p-8">Login — coming in Batch 4</div> },
      { path: 'register', element: <div className="p-8">Register — coming in Batch 4</div> },
    ],
  },
])
