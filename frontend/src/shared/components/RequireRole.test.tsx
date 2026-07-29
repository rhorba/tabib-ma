import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { RequireRole } from './RequireRole'

function renderGuardedRoute(roles: ('DOCTOR' | 'PLATFORM_ADMIN')[]) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<div>Login page</div>} />
      <Route path="/" element={<div>Home page</div>} />
      <Route element={<RequireRole roles={roles} />}>
        <Route path="/protected" element={<div>Protected content</div>} />
      </Route>
    </Routes>,
    { initialEntries: ['/protected'] }
  )
}

describe('RequireRole', () => {
  it('redirects to /login when unauthenticated', async () => {
    renderGuardedRoute(['DOCTOR'])

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('redirects to / when the user role does not match', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderGuardedRoute(['DOCTOR'])

    expect(await screen.findByText('Home page')).toBeInTheDocument()
  })

  it('renders the protected content when the role matches', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderGuardedRoute(['DOCTOR'])

    expect(await screen.findByText('Protected content')).toBeInTheDocument()
  })
})
