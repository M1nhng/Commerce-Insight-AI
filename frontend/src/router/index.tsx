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
import { ProductsPage } from '@/features/products/pages/ProductsPage'
import { CategoriesPage } from '@/features/products/pages/CategoriesPage'
import { InventoryPage } from '@/features/inventory/pages/InventoryPage'
import { WarehousePage } from '@/features/inventory/pages/WarehousePage'

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

          // ── Sprint 6: Product & Category ───────────────────────────
          {
            path: '/products',
            element: <ProductsPage />,
          },
          {
            path: '/categories',
            element: <CategoriesPage />,
          },

          // ── Sprint 8: Inventory & Warehouses ───────────────────────
          {
            path: '/inventory',
            element: <InventoryPage />,
          },
          {
            path: '/warehouses',
            element: <WarehousePage />,
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
