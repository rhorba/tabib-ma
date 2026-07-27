// Access token lives in memory only (never persisted) to limit its exposure
// window — it's short-lived (15 min) anyway. The refresh token is the only
// thing persisted (rotated on every use, revocable server-side), so a page
// reload can silently re-establish a session via POST /auth/refresh.
const REFRESH_TOKEN_STORAGE_KEY = 'tabibma-refresh-token'

let accessToken: string | null = null

export const tokenStore = {
  getAccessToken: () => accessToken,
  setAccessToken: (token: string | null) => {
    accessToken = token
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
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
  },
}
