import createClient from 'openapi-fetch'
import type { paths } from './schema'
import { tokenStore } from './tokenStore'

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
  onRequest({ request }) {
    const accessToken = tokenStore.getAccessToken()
    if (accessToken) {
      request.headers.set('Authorization', `Bearer ${accessToken}`)
    }
    return request
  },
})
