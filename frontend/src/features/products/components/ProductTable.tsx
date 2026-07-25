/**
 * features/products/components/ProductTable.tsx
 *
 * Data table for products with sort, actions dropdown, and skeleton loading.
 * Design spec §10.1
 */
import { useState } from 'react'
import { MoreHorizontal, Pencil, Trash2, Package } from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/common/StatusBadge'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { TableSkeleton } from '@/components/common/TableSkeleton'
import { useDeleteProduct } from '../hooks/useProducts'
import type { ProductSummaryResponse } from '@/types/product.types'

interface ProductTableProps {
  products: ProductSummaryResponse[]
  isLoading: boolean
  onEdit: (product: ProductSummaryResponse) => void
}

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)

const formatDate = (iso: string) =>
  new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(
    new Date(iso)
  )

export function ProductTable({ products, isLoading, onEdit }: ProductTableProps) {
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const deleteMutation = useDeleteProduct()

  const handleConfirmDelete = async () => {
    if (!deleteId) return
    await deleteMutation.mutateAsync(deleteId)
    setDeleteId(null)
  }

  if (isLoading) return <TableSkeleton rows={5} cols={8} />

  if (products.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center py-20 rounded-xl border"
        style={{ borderColor: 'var(--border-subtle)', background: 'var(--bg-surface)' }}
      >
        <div
          className="mb-4 flex h-16 w-16 items-center justify-center rounded-full"
          style={{ background: 'var(--bg-elevated)' }}
        >
          <Package className="h-8 w-8" style={{ color: 'var(--text-muted)' }} />
        </div>
        <p className="text-heading-4" style={{ color: 'var(--text-primary)' }}>
          No products yet
        </p>
        <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
          Add your first product to start tracking inventory.
        </p>
      </div>
    )
  }

  return (
    <>
      <div
        className="rounded-xl border overflow-hidden"
        style={{ borderColor: 'var(--border-default)' }}
      >
        <div className="overflow-x-auto">
          <table className="w-full text-body-sm" style={{ borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-default)' }}>
                {['Name', 'SKU', 'Category', 'Price', 'Stock', 'Status', 'Created', ''].map((h) => (
                  <th
                    key={h}
                    className="px-4 py-3 text-left font-semibold text-caption tracking-wide"
                    style={{ color: 'var(--text-muted)' }}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {products.map((product, idx) => (
                <tr
                  key={product.id}
                  className="group transition-colors"
                  style={{
                    background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
                    borderBottom: '1px solid var(--border-subtle)',
                    cursor: 'pointer',
                  }}
                  onMouseEnter={(e) => {
                    ;(e.currentTarget as HTMLTableRowElement).style.background = 'var(--bg-overlay)'
                  }}
                  onMouseLeave={(e) => {
                    ;(e.currentTarget as HTMLTableRowElement).style.background =
                      idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)'
                  }}
                >
                  {/* Name + Image */}
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      {product.imageUrl ? (
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="h-9 w-9 rounded-lg object-cover shrink-0"
                          style={{ border: '1px solid var(--border-default)' }}
                          onError={(e) => {
                            ;(e.currentTarget as HTMLImageElement).style.display = 'none'
                          }}
                        />
                      ) : (
                        <div
                          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
                          style={{ background: 'var(--bg-overlay)' }}
                        >
                          <Package className="h-4 w-4" style={{ color: 'var(--text-muted)' }} />
                        </div>
                      )}
                      <div>
                        <p className="font-medium text-body-sm" style={{ color: 'var(--text-primary)' }}>
                          {product.name}
                        </p>
                      </div>
                    </div>
                  </td>

                  {/* SKU */}
                  <td className="px-4 py-3">
                    <code className="text-code" style={{ color: 'var(--accent-400)' }}>
                      {product.sku}
                    </code>
                  </td>

                  {/* Category */}
                  <td className="px-4 py-3" style={{ color: 'var(--text-secondary)' }}>
                    {product.categoryName ?? (
                      <span style={{ color: 'var(--text-muted)' }}>—</span>
                    )}
                  </td>

                  {/* Price */}
                  <td className="px-4 py-3 font-medium" style={{ color: 'var(--text-primary)' }}>
                    {formatCurrency(product.price)}
                  </td>

                  {/* Stock */}
                  <td className="px-4 py-3">
                    <span
                      className="font-medium"
                      style={{
                        color:
                          product.stockQuantity === 0
                            ? 'var(--error)'
                            : product.stockQuantity < 10
                            ? 'var(--warning)'
                            : 'var(--text-primary)',
                      }}
                    >
                      {product.stockQuantity}
                      {product.stockQuantity < 10 && product.stockQuantity > 0 && (
                        <span className="ml-1 text-caption" style={{ color: 'var(--warning)' }}>⚠</span>
                      )}
                    </span>
                  </td>

                  {/* Status */}
                  <td className="px-4 py-3">
                    <StatusBadge status={product.active} />
                  </td>

                  {/* Created */}
                  <td className="px-4 py-3 text-caption" style={{ color: 'var(--text-muted)' }}>
                    {formatDate(product.createdAt)}
                  </td>

                  {/* Actions */}
                  <td className="px-4 py-3">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                          style={{ color: 'var(--text-muted)' }}
                          aria-label={`Actions for ${product.name}`}
                        >
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent
                        align="end"
                        style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
                      >
                        <DropdownMenuItem
                          onClick={() => onEdit(product)}
                          style={{ color: 'var(--text-primary)' }}
                          className="cursor-pointer"
                        >
                          <Pencil className="mr-2 h-4 w-4" />
                          Edit
                        </DropdownMenuItem>
                        <DropdownMenuSeparator style={{ background: 'var(--border-subtle)' }} />
                        <DropdownMenuItem
                          onClick={() => setDeleteId(product.id)}
                          className="cursor-pointer"
                          style={{ color: 'var(--error)' }}
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmDialog
        open={!!deleteId}
        onOpenChange={(open) => !open && setDeleteId(null)}
        title="Delete Product?"
        description="This product will be soft-deleted and removed from the catalog. This action can be undone by an administrator."
        confirmLabel="Delete Product"
        loading={deleteMutation.isPending}
        onConfirm={handleConfirmDelete}
      />
    </>
  )
}
