/**
 * App.tsx — Root Application Component
 *
 * Provider stack (outer → inner):
 *   ThemeProvider      — dark/light theme, [data-theme] on <html>
 *   QueryProvider      — TanStack Query client + devtools
 *   AuthProvider       — startup token validation + session restore
 *   Toaster            — react-hot-toast notification layer
 *   AppRouter          — React Router with protected routes
 */
import { ThemeProvider } from '@/providers/ThemeProvider'
import { QueryProvider } from '@/providers/QueryProvider'
import { AuthProvider } from '@/providers/AuthProvider'
import { AppRouter } from '@/router'
import { Toaster } from 'react-hot-toast'

function App() {
  return (
    <ThemeProvider defaultTheme="dark">
      <QueryProvider>
        <AuthProvider>
          {/* Global toast notifications */}
          <Toaster
            position="top-right"
            gutter={8}
            toastOptions={{
              duration: 4000,
              style: {
                background: 'var(--bg-elevated)',
                color: 'var(--text-primary)',
                border: '1px solid var(--border-default)',
                borderRadius: '0.75rem',
                fontSize: '0.875rem',
                fontFamily: 'Inter, system-ui, sans-serif',
                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)',
              },
              success: {
                iconTheme: {
                  primary: 'var(--success)',
                  secondary: 'var(--bg-elevated)',
                },
              },
              error: {
                iconTheme: {
                  primary: 'var(--error)',
                  secondary: 'var(--bg-elevated)',
                },
              },
            }}
          />
          <AppRouter />
        </AuthProvider>
      </QueryProvider>
    </ThemeProvider>
  )
}

export default App
