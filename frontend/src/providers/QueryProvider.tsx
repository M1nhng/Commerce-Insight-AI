/**
 * providers/QueryProvider.tsx — TanStack Query configuration.
 *
 * Global defaults:
 * - staleTime: 30s (most data is fresh for 30s before refetch)
 * - retry: 1 (single retry on failure)
 * - refetchOnWindowFocus: false (don't refetch aggressively on tab switch)
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import type { ReactNode } from 'react'

/**
 * Never auto-retry client errors — a 401 is handled by the Axios interceptor,
 * and 403 / 404 / 409 / 413 / 415 / 429 will not succeed on a blind repeat.
 * Only transient 5xx / network failures get a single retry.
 */
function retryQuery(failureCount: number, error: unknown): boolean {
  const status = (error as { response?: { status?: number } })?.response?.status
  if (status && status >= 400 && status < 500) return false
  return failureCount < 1
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,       // 30 seconds
      gcTime: 5 * 60 * 1000,      // 5 minutes GC (formerly cacheTime)
      retry: retryQuery,
      refetchOnWindowFocus: false,
    },
    mutations: {
      // Mutations (order creation, imports, status changes) are never retried
      // automatically — at most one retry happens after a token refresh, in
      // the Axios interceptor.
      retry: 0,
    },
  },
})

interface QueryProviderProps {
  children: ReactNode
}

export function QueryProvider({ children }: QueryProviderProps) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {import.meta.env.DEV && (
        <ReactQueryDevtools initialIsOpen={false} position="bottom" />
      )}
    </QueryClientProvider>
  )
}

export { queryClient }
