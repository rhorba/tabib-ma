import createClient from 'openapi-fetch'
import type { paths } from './schema'
import { tokenStore } from './tokenStore'

export const apiClient = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL,
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
