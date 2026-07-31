import createClient from 'openapi-fetch'
import type { paths } from './schema'
import { tokenStore } from './tokenStore'

const AUTH_PATH_SEGMENT = '/api/v1/auth/'

export const apiClient = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL,
  // openapi-fetch resolves `fetch` once, at client-creation time, via a
  // default parameter (`baseFetch = globalThis.fetch`). Wrapping it in a
  // function defers that lookup to request time instead — otherwise MSW's
  // test-time fetch patch (applied later, in a test `beforeAll`) never
  // takes effect, since the client already captured the original fetch.
  fetch: (...args) => globalThis.fetch(...args),
})

apiClient.use({
  async onRequest({ request }) {
    // /auth/* never needs (or accepts) a bearer token, and refresh/login are
    // themselves what AuthContext's bootstrap uses to resolve the access
    // token — waiting here would deadlock waiting on itself. Every other
    // endpoint waits for that bootstrap to settle first: a query firing on
    // mount (any hard navigation/reload) used to race the async bootstrap
    // and get a 401 with nothing retrying it.
    if (!request.url.includes(AUTH_PATH_SEGMENT)) {
      await tokenStore.waitUntilReady()
    }
    const accessToken = tokenStore.getAccessToken()
    if (accessToken) {
      request.headers.set('Authorization', `Bearer ${accessToken}`)
    }
    return request
  },
  onResponse({ request, response }) {
    // A 401 outside of /auth/* means the access token AuthContext handed us
    // is invalid despite the wait above (expired mid-session and the
    // proactive refresh — see AuthContext's onSessionRefreshed scheduling —
    // didn't run in time, e.g. a backgrounded tab throttling its timer).
    // Flip the app's auth state so it reflects reality instead of leaving
    // protected pages stuck rendering a stale "authenticated" view against
    // requests that will keep failing.
    if (response.status === 401 && !request.url.includes(AUTH_PATH_SEGMENT)) {
      tokenStore.clear()
      tokenStore.notifySessionExpired()
    }
    return response
  },
})
