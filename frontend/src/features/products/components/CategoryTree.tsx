/**
 * features/products/components/CategoryTree.tsx
 *
 * Hierarchical category tree with collapsible nodes, product count badges,
 * and inline edit/delete actions.
 */
import { useState } from 'react'
import {
  ChevronRight,
  ChevronDown,
  FolderOpen,
  Folder,
  MoreHorizontal,
  Pencil,
  Trash2,
  Plus,
} from 'lucide-react'
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
import { useDeleteCategory } from '../hooks/useCategories'
import type { CategoryTreeResponse } from '@/types/product.types'

// ── Tree Node ─────────────────────────────────────────────────────────────

interface TreeNodeProps {
  node: CategoryTreeResponse
  depth: number
  onEdit: (category: CategoryTreeResponse) => void
  onAddChild: (parentId: string) => void
}

function TreeNode({ node, depth, onEdit, onAddChild }: TreeNodeProps) {
  const [expanded, setExpanded] = useState(depth === 0)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const deleteMutation = useDeleteCategory()
  const hasChildren = node.children && node.children.length > 0

  return (
    <>
      <div
        className="group flex items-center gap-2 px-3 py-2.5 rounded-lg transition-colors"
        style={{
          marginLeft: `${depth * 20}px`,
          background: 'transparent',
        }}
        onMouseEnter={(e) => {
          ;(e.currentTarget as HTMLDivElement).style.background = 'var(--bg-overlay)'
        }}
        onMouseLeave={(e) => {
          ;(e.currentTarget as HTMLDivElement).style.background = 'transparent'
        }}
      >
        {/* Expand toggle */}
        <button
          onClick={() => setExpanded((e) => !e)}
          className="flex h-5 w-5 shrink-0 items-center justify-center rounded transition-colors"
          style={{ color: hasChildren ? 'var(--text-secondary)' : 'transparent' }}
          aria-label={expanded ? 'Collapse' : 'Expand'}
          disabled={!hasChildren}
        >
          {hasChildren ? (
            expanded ? (
              <ChevronDown className="h-3.5 w-3.5" />
            ) : (
              <ChevronRight className="h-3.5 w-3.5" />
            )
          ) : (
            <span className="h-3.5 w-3.5" />
          )}
        </button>

        {/* Folder icon */}
        {expanded && hasChildren ? (
          <FolderOpen className="h-4 w-4 shrink-0" style={{ color: 'var(--accent-400)' }} />
        ) : (
          <Folder className="h-4 w-4 shrink-0" style={{ color: hasChildren ? 'var(--accent-500)' : 'var(--text-muted)' }} />
        )}

        {/* Name */}
        <span
          className="flex-1 text-body-sm font-medium truncate"
          style={{ color: 'var(--text-primary)' }}
        >
          {node.name}
        </span>

        {/* Metadata pills */}
        <div className="flex items-center gap-2 ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
          {/* Product count */}
          {node.productCount > 0 && (
            <span
              className="text-caption px-2 py-0.5 rounded-full"
              style={{ background: 'var(--bg-overlay)', color: 'var(--text-secondary)' }}
            >
              {node.productCount} products
            </span>
          )}

          {/* Status */}
          <StatusBadge status={node.active} />

          {/* Sort order */}
          <span className="text-caption" style={{ color: 'var(--text-muted)' }}>
            #{node.sortOrder}
          </span>
        </div>

        {/* Actions */}
        <div className="opacity-0 group-hover:opacity-100 transition-opacity">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                style={{ color: 'var(--text-muted)' }}
                aria-label={`Actions for ${node.name}`}
              >
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
            >
              <DropdownMenuItem
                onClick={() => onAddChild(node.id)}
                style={{ color: 'var(--text-primary)' }}
                className="cursor-pointer"
              >
                <Plus className="mr-2 h-4 w-4" />
                Add subcategory
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => onEdit(node)}
                style={{ color: 'var(--text-primary)' }}
                className="cursor-pointer"
              >
                <Pencil className="mr-2 h-4 w-4" />
                Edit
              </DropdownMenuItem>
              <DropdownMenuSeparator style={{ background: 'var(--border-subtle)' }} />
              <DropdownMenuItem
                onClick={() => setDeleteId(node.id)}
                className="cursor-pointer"
                style={{ color: 'var(--error)' }}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Children */}
      {expanded && hasChildren && (
        <div>
          {node.children.map((child) => (
            <TreeNode
              key={child.id}
              node={child}
              depth={depth + 1}
              onEdit={onEdit}
              onAddChild={onAddChild}
            />
          ))}
        </div>
      )}

      {/* Confirm delete */}
      <ConfirmDialog
        open={!!deleteId}
        onOpenChange={(open) => !open && setDeleteId(null)}
        title={`Delete "${node.name}"?`}
        description="This category will be deleted. It must have no active products or child categories."
        confirmLabel="Delete Category"
        loading={deleteMutation.isPending}
        onConfirm={async () => {
          if (deleteId) {
            await deleteMutation.mutateAsync(deleteId)
            setDeleteId(null)
          }
        }}
      />
    </>
  )
}

// ── Category Tree ─────────────────────────────────────────────────────────

interface CategoryTreeProps {
  tree: CategoryTreeResponse[]
  isLoading: boolean
  onEdit: (category: CategoryTreeResponse) => void
  onAddChild: (parentId: string) => void
}

export function CategoryTree({ tree, isLoading, onEdit, onAddChild }: CategoryTreeProps) {
  if (isLoading) {
    return <TableSkeleton rows={6} cols={4} />
  }

  if (tree.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center py-16 rounded-xl border"
        style={{ borderColor: 'var(--border-subtle)', background: 'var(--bg-surface)' }}
      >
        <Folder className="h-12 w-12 mb-4" style={{ color: 'var(--text-muted)' }} />
        <p className="text-heading-4" style={{ color: 'var(--text-primary)' }}>
          No categories yet
        </p>
        <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
          Create your first category to organise your products.
        </p>
      </div>
    )
  }

  return (
    <div
      className="rounded-xl border p-2"
      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
    >
      {/* Header row */}
      <div
        className="flex items-center gap-2 px-3 py-2 mb-1 border-b"
        style={{ borderColor: 'var(--border-subtle)' }}
      >
        <span className="flex-1 text-caption font-semibold tracking-widest uppercase" style={{ color: 'var(--text-muted)' }}>
          Name
        </span>
        <span className="text-caption font-semibold tracking-widest uppercase" style={{ color: 'var(--text-muted)' }}>
          Products / Status / Order
        </span>
      </div>

      {tree.map((node) => (
        <TreeNode
          key={node.id}
          node={node}
          depth={0}
          onEdit={onEdit}
          onAddChild={onAddChild}
        />
      ))}
    </div>
  )
}
