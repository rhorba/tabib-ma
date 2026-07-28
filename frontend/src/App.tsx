import { QueryClientProvider } from '@tanstack/react-query'
import { Direction } from 'radix-ui'
import { RouterProvider } from 'react-router'
import { AuthProvider } from './features/auth/AuthContext'
import { queryClient } from './app/queryClient'
import { router } from './app/routes'
import { useSyncHtmlDir } from './shared/i18n/useSyncHtmlDir'

function App() {
  const dir = useSyncHtmlDir()

  return (
    <Direction.Provider dir={dir}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </QueryClientProvider>
    </Direction.Provider>
  )
}

export default App
