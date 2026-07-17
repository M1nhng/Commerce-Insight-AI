/**
 * router/index.tsx — Application Router Definition
 */
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleGuard } from './RoleGuard'
import { AppShell } from '@/components/layout/AppShell'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { ProfilePage } from '@/features/auth/pages/ProfilePage'
import { DashboardPlaceholder } from '@/features/dashboard/DashboardPlaceholder'

const router = createBrowserRouter([
  // ── Public routes ──────────────────────────────────────────────────────
  {
    path: '/login',
    element: <LoginPage />,
  },

  // ── Protected routes (require authentication) ─────────────────────────
  {
    element: <ProtectedRoute />,
    children: [
      {
        // All protected pages share the AppShell (sidebar + header)
        element: <AppShell />,
        children: [
          {
            path: '/',
            element: <DashboardPlaceholder />,
          },
          {
            path: '/dashboard',
            element: <DashboardPlaceholder />,
          },
          {
            path: '/profile',
            element: <ProfilePage />,
          },

          // Admin-only routes
          {
            path: '/admin',
            element: (
              <RoleGuard roles={['ADMIN']}>
                <DashboardPlaceholder />
              </RoleGuard>
            ),
          },

          // Future module routes added in subsequent sprints:
          // { path: '/products',   element: <ProductsPage /> },
          // { path: '/orders',     element: <OrdersPage /> },
          // { path: '/analytics',  element: <AnalyticsPage /> },
          // { path: '/customers',  element: <CustomersPage /> },
        ],
      },
    ],
  },

  // ── Catch-all redirect ────────────────────────────────────────────────
  {
    path: '*',
    element: <LoginPage />,
  },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
