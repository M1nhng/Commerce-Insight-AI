/**
 * router/index.tsx — Application Router Definition
 *
 * Sprint 14: page-level components are route-split with React.lazy so the initial
 * JS bundle only carries the shell (router, guards, AppShell, providers, auth
 * store) + the Login page. Every authenticated page is fetched on demand and
 * rendered inside a single <Suspense> boundary in AppShell (fallback: AppLoader).
 *
 * NOT lazied (must be present to bootstrap / gate auth):
 *   ProtectedRoute, RoleGuard, AppShell, LoginPage, providers, auth store.
 * Lazy loading never bypasses auth — ProtectedRoute/RoleGuard still resolve
 * before the lazy element mounts.
 */
import { lazy } from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleGuard } from './RoleGuard'
import { AppShell } from '@/components/layout/AppShell'
import { LoginPage } from '@/features/auth/pages/LoginPage'

// ── Lazily-loaded pages (named exports → default for React.lazy) ─────────
const ProfilePage = lazy(() =>
  import('@/features/auth/pages/ProfilePage').then((m) => ({ default: m.ProfilePage })),
)
const DashboardPlaceholder = lazy(() =>
  import('@/features/dashboard/DashboardPlaceholder').then((m) => ({ default: m.DashboardPlaceholder })),
)
const ProductsPage = lazy(() =>
  import('@/features/products/pages/ProductsPage').then((m) => ({ default: m.ProductsPage })),
)
const CategoriesPage = lazy(() =>
  import('@/features/products/pages/CategoriesPage').then((m) => ({ default: m.CategoriesPage })),
)
const InventoryPage = lazy(() =>
  import('@/features/inventory/pages/InventoryPage').then((m) => ({ default: m.InventoryPage })),
)
const WarehousePage = lazy(() =>
  import('@/features/inventory/pages/WarehousePage').then((m) => ({ default: m.WarehousePage })),
)
const CustomersPage = lazy(() =>
  import('@/features/customers/pages/CustomersPage').then((m) => ({ default: m.CustomersPage })),
)
const CreateCustomerPage = lazy(() =>
  import('@/features/customers/pages/CreateCustomerPage').then((m) => ({ default: m.CreateCustomerPage })),
)
const EditCustomerPage = lazy(() =>
  import('@/features/customers/pages/EditCustomerPage').then((m) => ({ default: m.EditCustomerPage })),
)
const CustomerDetailPage = lazy(() =>
  import('@/features/customers/pages/CustomerDetailPage').then((m) => ({ default: m.CustomerDetailPage })),
)
const CustomerGroupsPage = lazy(() =>
  import('@/features/customers/pages/CustomerGroupsPage').then((m) => ({ default: m.CustomerGroupsPage })),
)
const OrdersPage = lazy(() =>
  import('@/features/orders/pages/OrdersPage').then((m) => ({ default: m.OrdersPage })),
)
const CreateOrderPage = lazy(() =>
  import('@/features/orders/pages/CreateOrderPage').then((m) => ({ default: m.CreateOrderPage })),
)
const OrderDetailPage = lazy(() =>
  import('@/features/orders/pages/OrderDetailPage').then((m) => ({ default: m.OrderDetailPage })),
)
const AnalyticsPage = lazy(() =>
  import('@/features/analytics').then((m) => ({ default: m.AnalyticsPage })),
)
const ImportPage = lazy(() =>
  import('@/features/import').then((m) => ({ default: m.ImportPage })),
)
const ImportJobsPage = lazy(() =>
  import('@/features/import').then((m) => ({ default: m.ImportJobsPage })),
)
const ImportJobDetailPage = lazy(() =>
  import('@/features/import').then((m) => ({ default: m.ImportJobDetailPage })),
)
const ExportPage = lazy(() =>
  import('@/features/export').then((m) => ({ default: m.ExportPage })),
)

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
        // All protected pages share the AppShell (sidebar + header). AppShell
        // wraps its <Outlet> in a single <Suspense> for the lazy pages below.
        element: <AppShell />,
        children: [
          {
            // Sprint 13D: the real analytics dashboard is the landing page.
            // Sprint 14: heading + breadcrumb both read "Dashboard" here.
            path: '/',
            element: <AnalyticsPage title="Dashboard" />,
          },
          {
            path: '/dashboard',
            element: <AnalyticsPage title="Dashboard" />,
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

          // ── Sprint 7: Customer Management ──────────────────────────
          {
            path: '/customers',
            element: <CustomersPage />,
          },
          {
            path: '/customers/new',
            element: <CreateCustomerPage />,
          },
          {
            path: '/customers/groups',
            element: <CustomerGroupsPage />,
          },
          {
            path: '/customers/:id',
            element: <CustomerDetailPage />,
          },
          {
            path: '/customers/:id/edit',
            element: <EditCustomerPage />,
          },

          // ── Sprint 8: Order Management ─────────────────────────────
          {
            path: '/orders',
            element: <OrdersPage />,
          },
          {
            path: '/orders/new',
            element: <CreateOrderPage />,
          },
          {
            path: '/orders/:id',
            element: <OrderDetailPage />,
          },

          // ── Sprint 9B: Analytics Dashboard ─────────────────────────
          {
            path: '/analytics',
            element: <AnalyticsPage title="Analytics" />,
          },

          // ── Sprint 10B: Data Import ────────────────────────────────
          {
            path: '/import',
            element: <ImportPage />,
          },
          {
            path: '/import/jobs',
            element: <ImportJobsPage />,
          },
          {
            path: '/import/jobs/:id',
            element: <ImportJobDetailPage />,
          },

          // ── Sprint 11B: Export Reports ─────────────────────────────
          {
            path: '/export',
            element: <ExportPage />,
          },

          // ── Admin-only routes ──────────────────────────────────────
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
