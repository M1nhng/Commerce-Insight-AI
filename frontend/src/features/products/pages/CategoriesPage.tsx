/**
 * features/products/pages/CategoriesPage.tsx
 *
 * Category management page with tree view, search, and CRUD.
 */
import { useState } from 'react'
import { Plus, Search, TreePine } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { CategoryTree } from '../components/CategoryTree'
import { CategoryForm } from '../components/CategoryForm'
import { useCategoryTree } from '../hooks/useCategories'
import { useAuth } from '@/hooks/useAuth'
import type { CategoryTreeResponse } from '@/types/product.types'

export function CategoriesPage() {
  const { isAtLeast } = useAuth()
  const canWrite = isAtLeast('MANAGER')

  const [search, setSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<CategoryTreeResponse | null>(null)
  const [defaultParentId, setDefaultParentId] = useState<string | null>(null)

  const { data: tree = [], isLoading } = useCategoryTree()

  // Client-side filter of tree nodes by name
  const filterTree = (nodes: CategoryTreeResponse[], q: string): CategoryTreeResponse[] => {
    if (!q) return nodes
    const lower = q.toLowerCase()
    return nodes
      .map((node) => {
        const childMatches = filterTree(node.children, q)
        if (node.name.toLowerCase().includes(lower) || childMatches.length > 0) {
          return { ...node, children: childMatches }
        }
        return null
      })
      .filter(Boolean) as CategoryTreeResponse[]
  }

  const filteredTree = filterTree(tree, search)

  const handleCreate = () => {
    setEditing(null)
    setDefaultParentId(null)
    setFormOpen(true)
  }

  const handleAddChild = (parentId: string) => {
    setEditing(null)
    setDefaultParentId(parentId)
    setFormOpen(true)
  }

  const handleEdit = (cat: CategoryTreeResponse) => {
    setEditing(cat)
    setDefaultParentId(null)
    setFormOpen(true)
  }

  const handleFormClose = (open: boolean) => {
    setFormOpen(open)
    if (!open) {
      setEditing(null)
      setDefaultParentId(null)
    }
  }

  // Total categories (flatten)
  const countAll = (nodes: CategoryTreeResponse[]): number =>
    nodes.reduce((sum, n) => sum + 1 + countAll(n.children), 0)

  return (
    <div className="animate-fade-in space-y-6">
      {/* ── Page Header ───────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-1" style={{ color: 'var(--text-primary)' }}>
            Categories
          </h1>
          <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
            {countAll(tree)} categories in {tree.length} root groups
          </p>
        </div>

        {canWrite && (
          <Button
            onClick={handleCreate}
            className="gap-2"
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            <Plus className="h-4 w-4" />
            New Category
          </Button>
        )}
      </div>

      {/* ── Stats row ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        {[
          { label: 'Total Categories', value: countAll(tree), icon: TreePine },
          { label: 'Root Categories', value: tree.length, icon: TreePine },
          {
            label: 'Total Products',
            value: tree.reduce((s, n) => s + n.productCount, 0),
            icon: TreePine,
          },
        ].map(({ label, value }) => (
          <div
            key={label}
            className="rounded-xl border p-4"
            style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
          >
            <p className="text-caption" style={{ color: 'var(--text-secondary)' }}>{label}</p>
            <p className="mt-1 text-display-md font-bold" style={{ color: 'var(--text-primary)' }}>
              {value}
            </p>
          </div>
        ))}
      </div>

      {/* ── Search ────────────────────────────────────────────────────── */}
      <div className="relative max-w-sm">
        <Search
          className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4"
          style={{ color: 'var(--text-muted)' }}
        />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search categories..."
          className="pl-9"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
        />
      </div>

      {/* ── Category Tree ──────────────────────────────────────────────── */}
      <CategoryTree
        tree={filteredTree}
        isLoading={isLoading}
        onEdit={handleEdit}
        onAddChild={handleAddChild}
      />

      {/* ── Category Form ──────────────────────────────────────────────── */}
      <CategoryForm
        open={formOpen}
        onOpenChange={handleFormClose}
        category={
          editing
            ? editing
            : defaultParentId
            ? ({ parentId: defaultParentId } as any)
            : null
        }
      />
    </div>
  )
}
