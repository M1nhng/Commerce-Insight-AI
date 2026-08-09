/**
 * features/customers/components/CustomerTable.tsx
 * Data table for the customer list page — design spec §10.1
 */
import { useNavigate } from 'react-router-dom'
import {
  MoreHorizontal, Eye, Pencil, Trash2, ShieldBan, ShieldCheck, ShieldOff,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { TableSkeleton } from '@/components/common/TableSkeleton'
import { CustomerStatusBadge } from './CustomerStatusBadge'
import type { CustomerSummaryResponse, CustomerStatus } from '@/types/customer.types'

interface CustomerTableProps {
  customers: CustomerSummaryResponse[]
  isLoading: boolean
  canWrite: boolean
  canDelete: boolean
  onDelete: (customer: CustomerSummaryResponse) => void
  onStatusChange: (customer: CustomerSummaryResponse, status: CustomerStatus) => void
}

const COL_STYLE = { color: 'var(--text-secondary)', whiteSpace: 'nowrap' as const }

export function CustomerTable({
  customers,
  isLoading,
  canWrite,
  canDelete,
  onDelete,
  onStatusChange,
}: CustomerTableProps) {
  const navigate = useNavigate()

  if (isLoading) return <TableSkeleton rows={8} cols={8} />

  if (customers.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center py-20 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <div className="text-5xl mb-4">👥</div>
        <h3 className="text-heading-3 mb-1" style={{ color: 'var(--text-primary)' }}>
          No customers found
        </h3>
        <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
          Try adjusting your filters or add your first customer.
        </p>
      </div>
    )
  }

  return (
    <div
      className="rounded-xl border overflow-hidden"
      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
    >
      <div className="overflow-x-auto">
        <table className="w-full text-body-sm">
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)' }}>
              {['Code', 'Name', 'Email', 'Phone', 'Group', 'Status', 'Created', ''].map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left font-medium text-caption uppercase tracking-wide"
                  style={{ color: 'var(--text-muted)' }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {customers.map((c, idx) => (
              <tr
                key={c.id}
                className="transition-colors cursor-pointer hover:bg-[var(--bg-overlay)]"
                style={{
                  borderBottom: '1px solid var(--border-subtle)',
                  background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
                }}
                onClick={() => navigate(`/customers/${c.id}`)}
              >
                {/* Code */}
                <td className="px-4 py-3 font-mono text-caption" style={{ color: 'var(--accent-400)' }}>
                  {c.customerCode}
                </td>

                {/* Name */}
                <td className="px-4 py-3 font-medium" style={{ color: 'var(--text-primary)' }}>
                  {c.fullName}
                </td>

                {/* Email */}
                <td className="px-4 py-3" style={COL_STYLE}>
                  {c.email ?? <span style={{ color: 'var(--text-muted)' }}>—</span>}
                </td>

                {/* Phone */}
                <td className="px-4 py-3" style={COL_STYLE}>
                  {c.phone ?? <span style={{ color: 'var(--text-muted)' }}>—</span>}
                </td>

                {/* Group */}
                <td className="px-4 py-3" style={{ color: 'var(--text-secondary)' }}>
                  {c.groupName ? (
                    <span
                      className="px-2 py-0.5 rounded text-caption font-medium"
                      style={{ background: 'var(--bg-overlay)', color: 'var(--accent-400)' }}
                    >
                      {c.groupName}
                    </span>
                  ) : (
                    <span style={{ color: 'var(--text-muted)' }}>—</span>
                  )}
                </td>

                {/* Status */}
                <td className="px-4 py-3">
                  <CustomerStatusBadge status={c.status} />
                </td>

                {/* Created */}
                <td className="px-4 py-3 text-caption" style={{ color: 'var(--text-muted)' }}>
                  {new Date(c.createdAt).toLocaleDateString('en-GB', {
                    day: '2-digit', month: 'short', year: 'numeric',
                  })}
                </td>

                {/* Actions */}
                <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        style={{ color: 'var(--text-muted)' }}
                      >
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                      align="end"
                      style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
                    >
                      <DropdownMenuItem
                        className="gap-2 cursor-pointer"
                        style={{ color: 'var(--text-primary)' }}
                        onClick={() => navigate(`/customers/${c.id}`)}
                      >
                        <Eye className="h-4 w-4" /> View Detail
                      </DropdownMenuItem>

                      {canWrite && (
                        <DropdownMenuItem
                          className="gap-2 cursor-pointer"
                          style={{ color: 'var(--text-primary)' }}
                          onClick={() => navigate(`/customers/${c.id}/edit`)}
                        >
                          <Pencil className="h-4 w-4" /> Edit
                        </DropdownMenuItem>
                      )}

                      {canWrite && c.status !== 'ACTIVE' && (
                        <DropdownMenuItem
                          className="gap-2 cursor-pointer"
                          style={{ color: 'var(--success)' }}
                          onClick={() => onStatusChange(c, 'ACTIVE')}
                        >
                          <ShieldCheck className="h-4 w-4" /> Activate
                        </DropdownMenuItem>
                      )}
                      {canWrite && c.status !== 'INACTIVE' && (
                        <DropdownMenuItem
                          className="gap-2 cursor-pointer"
                          style={{ color: 'var(--text-muted)' }}
                          onClick={() => onStatusChange(c, 'INACTIVE')}
                        >
                          <ShieldOff className="h-4 w-4" /> Deactivate
                        </DropdownMenuItem>
                      )}
                      {canWrite && c.status !== 'BLOCKED' && (
                        <DropdownMenuItem
                          className="gap-2 cursor-pointer"
                          style={{ color: 'var(--warning)' }}
                          onClick={() => onStatusChange(c, 'BLOCKED')}
                        >
                          <ShieldBan className="h-4 w-4" /> Block
                        </DropdownMenuItem>
                      )}

                      {canDelete && (
                        <>
                          <DropdownMenuSeparator style={{ background: 'var(--border-subtle)' }} />
                          <DropdownMenuItem
                            className="gap-2 cursor-pointer"
                            style={{ color: 'var(--error)' }}
                            onClick={() => onDelete(c)}
                          >
                            <Trash2 className="h-4 w-4" /> Delete
                          </DropdownMenuItem>
                        </>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
