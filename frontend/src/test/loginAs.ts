import { tokenStore } from '@/shared/api/tokenStore'
import { seedUserAndIssueTokens, type FakeUser } from './authHandlers'

// Bypasses the real login flow for tests that only need an authenticated
// session, not to exercise AuthContext/login itself. Sets both tokens: the
// access token so apiClient calls succeed immediately, and the refresh token
// so AuthProvider's own bootstrap effect (which every renderWithProviders
// tree runs) settles into 'authenticated' — needed by anything reading
// useAuth() directly, like RequireRole.
export function loginAs(user: Omit<FakeUser, 'id'>) {
  const tokens = seedUserAndIssueTokens(user)
  tokenStore.setAccessToken(tokens.accessToken)
  tokenStore.setRefreshToken(tokens.refreshToken)
  return tokens
}
