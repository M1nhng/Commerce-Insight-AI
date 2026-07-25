/**
 * features/products/pages/ProductsPage.tsx
 *
 * Full product management page with search, filter, pagination, and CRUD.
 * Design spec §12.3
 */
import { useState, useCallback } from 'react'
import { Plus, Search, X, SlidersHorizontal } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ProductTable } from '../components/ProductTable'
import { ProductForm } from '../components/ProductForm'
import { useProducts } from '../hooks/useProducts'
import { useCategoryOptions } from '../hooks/useCategories'
import { useAuth } from '@/hooks/useAuth'
import type { ProductSummaryResponse, ProductFilterParams } from '@/types/product.types'
import { useProduct } from '../hooks/useProducts'

// ── Pagination component ──────────────────────────────────────────────────

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  size: number
  onPageChange: (p: number) => void
  onSizeChange: (s: number) => void
}

function Pagination({ page, totalPages, totalElements, size, onPageChange, onSizeChange }: PaginationProps) {
  const start = page * size + 1
  const end = Math.min((page + 1) * size, totalElements)

  return (
    <div className="flex items-center justify-between mt-4 flex-wrap gap-3">
      <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
        Showing <strong style={{ color: 'var(--text-primary)' }}>{totalElements === 0 ? 0 : start}–{end}</strong>{' '}
        of <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> products
      </p>

      <div className="flex items-center gap-3">
        {/* Page size */}
        <Select value={String(size)} onValueChange={(v) => { onSizeChange(Number(v)); onPageChange(0) }}>
          <SelectTrigger
            className="w-28 h-8 text-body-sm"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            {[10, 25, 50].map((s) => (
              <SelectItem key={s} value={String(s)} style={{ color: 'var(--text-primary)' }}>
                {s} / page
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Page buttons */}
        <div className="flex items-center gap-1">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            ← Prev
          </Button>

          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
            const p = totalPages <= 5 ? i : Math.max(0, Math.min(page - 2, totalPages - 5)) + i
            return (
              <Button
                key={p}
                variant={p === page ? 'default' : 'outline'}
                size="sm"
                onClick={() => onPageChange(p)}
                style={
                  p === page
                    ? { background: 'var(--accent-500)', color: '#fff', border: 'none' }
                    : { background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }
                }
              >
                {p + 1}
              </Button>
            )
          })}

          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            Next →
          </Button>
        </div>
      </div>
    </div>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────

export function ProductsPage() {
  const { isAtLeast } = useAuth()
  const canWrite = isAtLeast('MANAGER')

  // ── Filter state ──────────────────────────────────────────────────────
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [categoryId, setCategoryId] = useState<string>('__all__')
  const [activeFilter, setActiveFilter] = useState<string>('__all__')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)

  // Debounce search
  const handleSearchChange = useCallback((value: string) => {
    setSearch(value)
    const t = setTimeout(() => {
      setDebouncedSearch(value)
      setPage(0)
    }, 400)
    return () => clearTimeout(t)
  }, [])

  const filters: ProductFilterParams = {
    search: debouncedSearch || undefined,
    categoryId: categoryId !== '__all__' ? categoryId : undefined,
    active: activeFilter === '__all__' ? undefined : activeFilter === 'true',
    page,
    size,
    sort: 'createdAt,desc',
  }

  // ── Data ──────────────────────────────────────────────────────────────
  const { data, isLoading } = useProducts(filters)
  const { data: categoryOptions = [] } = useCategoryOptions()

  // ── Form state ────────────────────────────────────────────────────────
  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const { data: editingProduct } = useProduct(editingId)

  const handleEdit = (product: ProductSummaryResponse) => {
    setEditingId(product.id)
    setFormOpen(true)
  }

  const handleCreate = () => {
    setEditingId(null)
    setFormOpen(true)
  }

  const handleFormClose = (open: boolean) => {
    setFormOpen(open)
    if (!open) setEditingId(null)
  }

  const clearFilters = () => {
    setSearch('')
    setDebouncedSearch('')
    setCategoryId('__all__')
    setActiveFilter('__all__')
    setPage(0)
  }

  const hasFilters = debouncedSearch || categoryId !== '__all__' || activeFilter !== '__all__'

  return (
    <div className="animate-fade-in space-y-6">
      {/* ── Page Header ───────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-1" style={{ color: 'var(--text-primary)' }}>
            Products
          </h1>
          <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
            {data?.totalElements ?? 0} products in your catalog
          </p>
        </div>

        {canWrite && (
          <Button
            onClick={handleCreate}
            className="gap-2"
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            <Plus className="h-4 w-4" />
            Add Product
          </Button>
        )}
      </div>

      {/* ── Filter Bar ────────────────────────────────────────────────── */}
      <div
        className="flex flex-wrap gap-3 p-4 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4"
            style={{ color: 'var(--text-muted)' }}
          />
          <Input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by name or SKU..."
            className="pl-9"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
          />
        </div>

        {/* Category filter */}
        <Select value={categoryId} onValueChange={(v) => { setCategoryId(v); setPage(0) }}>
          <SelectTrigger
            className="w-44"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SlidersHorizontal className="h-3.5 w-3.5 mr-2 shrink-0" style={{ color: 'var(--text-muted)' }} />
            <SelectValue placeholder="All Categories" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value="__all__" style={{ color: 'var(--text-secondary)' }}>All Categories</SelectItem>
            {categoryOptions.map((opt) => (
              <SelectItem key={opt.value} value={opt.value} style={{ color: 'var(--text-primary)' }}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Status filter */}
        <Select value={activeFilter} onValueChange={(v) => { setActiveFilter(v); setPage(0) }}>
          <SelectTrigger
            className="w-36"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue placeholder="All Status" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value="__all__" style={{ color: 'var(--text-secondary)' }}>All Status</SelectItem>
            <SelectItem value="true" style={{ color: 'var(--success)' }}>Active</SelectItem>
            <SelectItem value="false" style={{ color: 'var(--text-muted)' }}>Inactive</SelectItem>
          </SelectContent>
        </Select>

        {/* Clear filters */}
        {hasFilters && (
          <Button
            variant="ghost"
            size="sm"
            onClick={clearFilters}
            className="gap-1.5"
            style={{ color: 'var(--text-muted)' }}
          >
            <X className="h-3.5 w-3.5" />
            Clear
          </Button>
        )}
      </div>

      {/* ── Product Table ─────────────────────────────────────────────── */}
      <ProductTable
        products={data?.content ?? []}
        isLoading={isLoading}
        onEdit={handleEdit}
      />

      {/* ── Pagination ────────────────────────────────────────────────── */}
      {data && data.totalPages > 0 && (
        <Pagination
          page={page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={setSize}
        />
      )}

      {/* ── Product Form (Sheet) ──────────────────────────────────────── */}
      <ProductForm
        open={formOpen}
        onOpenChange={handleFormClose}
        product={editingProduct ?? null}
      />
    </div>
  )
}
