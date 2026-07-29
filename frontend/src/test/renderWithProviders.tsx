import type { ReactElement, ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { AuthProvider } from '@/features/auth/AuthContext'

function makeProviders(initialEntries: string[]) {
  return function Providers({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })

    return (
      <MemoryRouter initialEntries={initialEntries}>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>{children}</AuthProvider>
        </QueryClientProvider>
      </MemoryRouter>
    )
  }
}

export function renderWithProviders(
  ui: ReactElement,
  options: { initialEntries?: string[] } = {}
) {
  return render(ui, { wrapper: makeProviders(options.initialEntries ?? ['/']) })
}

export * from '@testing-library/react'
