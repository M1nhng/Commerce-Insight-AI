/**
 * router/index.tsx — Application Router Definition
 */
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleGuard } from './RoleGuard'
import { LoginPage } from '@/features/auth/pages/LoginPage'
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
        path: '/',
        element: <DashboardPlaceholder />,
      },
      {
        path: '/dashboard',
        element: <DashboardPlaceholder />,
      },
      // Future module routes will be added here in subsequent sprints
      // e.g.:
      // { path: '/products', element: <ProductsPage /> },
      // { path: '/orders', element: <OrdersPage /> },

      // Admin-only routes
      {
        path: '/admin',
        element: (
          <RoleGuard roles={['ADMIN']}>
            <DashboardPlaceholder />
          </RoleGuard>
        ),
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
