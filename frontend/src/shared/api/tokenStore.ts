// Access token lives in memory only (never persisted) to limit its exposure
// window — it's short-lived (15 min) anyway. The refresh token is the only
// thing persisted (rotated on every use, revocable server-side), so a page
// reload can silently re-establish a session via POST /auth/refresh.
const REFRESH_TOKEN_STORAGE_KEY = 'tabibma-refresh-token'

let accessToken: string | null = null
let accessTokenExpiresInMs: number | null = null

// Resolves once the initial bootstrap has settled the access token one way or
// another (a token was set, or AuthContext determined there is none).
// apiClient awaits this before attaching Authorization so a query that fires
// on mount doesn't race the async bootstrap and get a 401 with nothing
// retrying it (the bug this exists to fix).
let isReady = false
let resolveReady: (() => void) | undefined
const readyPromise = new Promise<void>((resolve) => {
  resolveReady = resolve
})

function markReady() {
  if (!isReady) {
    isReady = true
    resolveReady?.()
  }
}

type SessionRefreshedListener = (expiresInMs: number | null) => void
type SessionExpiredListener = () => void

const sessionRefreshedListeners = new Set<SessionRefreshedListener>()
const sessionExpiredListeners = new Set<SessionExpiredListener>()

export const tokenStore = {
  getAccessToken: () => accessToken,
  getAccessTokenExpiresInMs: () => accessTokenExpiresInMs,
  setAccessToken: (token: string | null, expiresInMs: number | null = null) => {
    accessToken = token
    accessTokenExpiresInMs = token ? expiresInMs : null
    markReady()
    sessionRefreshedListeners.forEach((listener) => listener(accessTokenExpiresInMs))
  },
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY),
  setRefreshToken: (token: string | null) => {
    if (token) {
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, token)
    } else {
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
    }
  },
  clear: () => {
    accessToken = null
    accessTokenExpiresInMs = null
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
  },

  markReady,
  waitUntilReady: () => (isReady ? Promise.resolve() : readyPromise),

  /** Fired whenever a new access token is set (bootstrap, login, or a
   * proactive/reactive refresh) — AuthContext uses this to (re)schedule its
   * next proactive refresh at the new expiry, wherever the token came from. */
  onSessionRefreshed: (listener: SessionRefreshedListener) => {
    sessionRefreshedListeners.add(listener)
    return () => sessionRefreshedListeners.delete(listener)
  },
  /** Fired when a request comes back 401 outside of /auth/* (apiClient) or a
   * refresh attempt fails (AuthContext) — the session is definitively gone. */
  onSessionExpired: (listener: SessionExpiredListener) => {
    sessionExpiredListeners.add(listener)
    return () => sessionExpiredListeners.delete(listener)
  },
  notifySessionExpired: () => {
    sessionExpiredListeners.forEach((listener) => listener())
  },
}
