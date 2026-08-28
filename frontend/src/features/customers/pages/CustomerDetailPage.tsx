/**
 * features/customers/pages/CustomerDetailPage.tsx
 *
 * Full customer detail view: profile, status, group, addresses.
 * Actions: Edit, Change Status, Delete, Manage Addresses.
 */
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, Pencil, Trash2, User2, Mail, Phone, Calendar, Users,
  ShieldBan, ShieldCheck, ShieldOff, Plus,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { CardSkeleton } from '@/components/common/TableSkeleton'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { CustomerStatusBadge } from '../components/CustomerStatusBadge'
import { AddressCard } from '../components/AddressCard'
import { AddressForm, type AddressFormValues } from '../components/AddressForm'
import { useCustomer, useDeleteCustomer, useUpdateCustomerStatus } from '../hooks/useCustomers'
import {
  useCustomerAddresses, useAddAddress, useUpdateAddress,
  useDeleteAddress, useSetDefaultAddress,
} from '../hooks/useCustomerAddresses'
import { useAuth } from '@/hooks/useAuth'
import type { CustomerAddressResponse, CustomerStatus } from '@/types/customer.types'

// ── Info row helper ───────────────────────────────────────────────────────

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start gap-3 py-2.5 border-b last:border-0"
      style={{ borderColor: 'var(--border-subtle)' }}>
      <span className="mt-0.5 shrink-0" style={{ color: 'var(--accent-400)' }}>{icon}</span>
      <div className="flex-1 min-w-0">
        <p className="text-caption" style={{ color: 'var(--text-muted)' }}>{label}</p>
        <p className="text-body-sm font-medium mt-0.5 truncate" style={{ color: 'var(--text-primary)' }}>
          {value ?? <span style={{ color: 'var(--text-muted)' }}>—</span>}
        </p>
      </div>
    </div>
  )
}

// ── Main Component ────────────────────────────────────────────────────────

export function CustomerDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { isAtLeast } = useAuth()
  const canWrite = isAtLeast('MANAGER')
  const canDelete = isAtLeast('ADMIN')

  // ── Data ──────────────────────────────────────────────────────────────
  const { data: customer, isLoading } = useCustomer(id ?? null)
  const { data: addresses = [] }      = useCustomerAddresses(id ?? null)

  // ── Mutations ─────────────────────────────────────────────────────────
  const deleteCustomer   = useDeleteCustomer()
  const updateStatus     = useUpdateCustomerStatus()
  const addAddress       = useAddAddress()
  const updateAddress    = useUpdateAddress()
  const deleteAddress    = useDeleteAddress()
  const setDefaultAddr   = useSetDefaultAddress()

  // ── Local state ───────────────────────────────────────────────────────
  const [deleteOpen, setDeleteOpen]       = useState(false)
  const [addrFormOpen, setAddrFormOpen]   = useState(false)
  const [editingAddr, setEditingAddr]     = useState<CustomerAddressResponse | null>(null)
  const [deletingAddr, setDeletingAddr]   = useState<CustomerAddressResponse | null>(null)

  const handleDelete = () => {
    if (!id) return
    deleteCustomer.mutate(id, { onSuccess: () => navigate('/customers') })
  }

  const handleStatusChange = (status: CustomerStatus) => {
    if (!id) return
    updateStatus.mutate({ id, data: { status } })
  }

  const handleAddressSubmit = (values: AddressFormValues) => {
    if (!id) return
    if (editingAddr) {
      updateAddress.mutate(
        { customerId: id, addressId: editingAddr.id, data: values },
        { onSuccess: () => { setAddrFormOpen(false); setEditingAddr(null) } }
      )
    } else {
      addAddress.mutate(
        { customerId: id, data: { ...values, isDefault: values.isDefault ?? false } },
        { onSuccess: () => setAddrFormOpen(false) }
      )
    }
  }

  const handleDeleteAddress = () => {
    if (!id || !deletingAddr) return
    deleteAddress.mutate(
      { customerId: id, addressId: deletingAddr.id },
      { onSuccess: () => setDeletingAddr(null) }
    )
  }

  const handleSetDefault = (addr: CustomerAddressResponse) => {
    if (!id) return
    setDefaultAddr.mutate({ customerId: id, addressId: addr.id })
  }

  const openEditAddress = (addr: CustomerAddressResponse) => {
    setEditingAddr(addr)
    setAddrFormOpen(true)
  }

  const openNewAddress = () => {
    setEditingAddr(null)
    setAddrFormOpen(true)
  }

  // ── Loading ───────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="animate-fade-in space-y-6">
        <CardSkeleton className="h-16" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <CardSkeleton className="h-72" />
          <div className="lg:col-span-2 space-y-4">
            <CardSkeleton className="h-40" />
            <CardSkeleton className="h-40" />
          </div>
        </div>
      </div>
    )
  }

  if (!customer) {
    return (
      <div className="flex flex-col items-center justify-center py-20">
        <p style={{ color: 'var(--text-secondary)' }}>Customer not found.</p>
        <Button className="mt-4" onClick={() => navigate('/customers')}>Back to Customers</Button>
      </div>
    )
  }

  return (
    <div className="animate-fade-in space-y-6">
      {/* ── Header ─────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost" size="icon"
            onClick={() => navigate('/customers')}
            style={{ color: 'var(--text-muted)' }}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-heading-1" style={{ color: 'var(--text-primary)' }}>
              {customer.fullName}
            </h1>
            <p className="font-mono text-caption mt-0.5" style={{ color: 'var(--accent-400)' }}>
              {customer.customerCode}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {canWrite && (
            <Button
              variant="outline"
              onClick={() => navigate(`/customers/${id}/edit`)}
              className="gap-2"
              style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
            >
              <Pencil className="h-4 w-4" /> Edit
            </Button>
          )}

          {canWrite && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="outline"
                  style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
                >
                  Change Status ▾
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                {customer.status !== 'ACTIVE' && (
                  <DropdownMenuItem className="gap-2 cursor-pointer" style={{ color: 'var(--success)' }}
                    onClick={() => handleStatusChange('ACTIVE')}>
                    <ShieldCheck className="h-4 w-4" /> Activate
                  </DropdownMenuItem>
                )}
                {customer.status !== 'INACTIVE' && (
                  <DropdownMenuItem className="gap-2 cursor-pointer" style={{ color: 'var(--text-muted)' }}
                    onClick={() => handleStatusChange('INACTIVE')}>
                    <ShieldOff className="h-4 w-4" /> Deactivate
                  </DropdownMenuItem>
                )}
                {customer.status !== 'BLOCKED' && (
                  <DropdownMenuItem className="gap-2 cursor-pointer" style={{ color: 'var(--warning)' }}
                    onClick={() => handleStatusChange('BLOCKED')}>
                    <ShieldBan className="h-4 w-4" /> Block
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}

          {canDelete && (
            <Button
              variant="outline"
              onClick={() => setDeleteOpen(true)}
              className="gap-2"
              style={{ borderColor: 'var(--error)', color: 'var(--error)', background: 'var(--error-bg)' }}
            >
              <Trash2 className="h-4 w-4" /> Delete
            </Button>
          )}
        </div>
      </div>

      {/* ── Content grid ───────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* ── Profile Card ─────────────────────────────────────────────── */}
        <div
          className="p-6 rounded-xl border space-y-1"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
        >
          {/* Avatar */}
          <div className="flex flex-col items-center pb-5 mb-3 border-b" style={{ borderColor: 'var(--border-subtle)' }}>
            <div
              className="h-16 w-16 rounded-full flex items-center justify-center text-2xl font-bold mb-3"
              style={{ background: 'var(--accent-500)20', color: 'var(--accent-400)' }}
            >
              {customer.firstName[0]}{customer.lastName[0]}
            </div>
            <p className="font-semibold text-heading-4" style={{ color: 'var(--text-primary)' }}>
              {customer.fullName}
            </p>
            <div className="mt-2">
              <CustomerStatusBadge status={customer.status} />
            </div>
          </div>

          <InfoRow icon={<User2 className="h-4 w-4" />} label="Customer Code" value={customer.customerCode} />
          <InfoRow icon={<Mail className="h-4 w-4" />}  label="Email"         value={customer.email} />
          <InfoRow icon={<Phone className="h-4 w-4" />} label="Phone"         value={customer.phone} />
          <InfoRow
            icon={<Calendar className="h-4 w-4" />}
            label="Date of Birth"
            value={customer.dateOfBirth
              ? new Date(customer.dateOfBirth).toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric' })
              : null}
          />
          <InfoRow
            icon={<User2 className="h-4 w-4" />}
            label="Gender"
            value={customer.gender?.replace(/_/g, ' ')}
          />
          <InfoRow
            icon={<Users className="h-4 w-4" />}
            label="Customer Group"
            value={customer.groupName
              ? <span style={{ color: 'var(--accent-400)' }}>{customer.groupName}</span>
              : null}
          />
          <div className="pt-3 mt-2 border-t text-caption" style={{ borderColor: 'var(--border-subtle)', color: 'var(--text-muted)' }}>
            Created {new Date(customer.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}
          </div>
        </div>

        {/* ── Right column ─────────────────────────────────────────────── */}
        <div className="lg:col-span-2 space-y-6">

          {/* Addresses */}
          <div
            className="p-6 rounded-xl border"
            style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
          >
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>
                Addresses
              </h2>
              {canWrite && (
                <Button
                  size="sm"
                  onClick={openNewAddress}
                  className="gap-2"
                  style={{ background: 'var(--accent-500)', color: '#fff' }}
                >
                  <Plus className="h-3.5 w-3.5" /> Add Address
                </Button>
              )}
            </div>

            {addresses.length === 0 ? (
              <div className="flex flex-col items-center py-10">
                <p className="text-4xl mb-2">📍</p>
                <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
                  No addresses yet.
                </p>
                {canWrite && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="mt-3 gap-2"
                    onClick={openNewAddress}
                    style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
                  >
                    <Plus className="h-3.5 w-3.5" /> Add first address
                  </Button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {addresses.map((addr) => (
                  <AddressCard
                    key={addr.id}
                    address={addr}
                    canWrite={canWrite}
                    onEdit={openEditAddress}
                    onDelete={setDeletingAddr}
                    onSetDefault={handleSetDefault}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Address Form Dialog ─────────────────────────────────────────── */}
      <AddressForm
        open={addrFormOpen}
        onOpenChange={(o) => { setAddrFormOpen(o); if (!o) setEditingAddr(null) }}
        address={editingAddr}
        isSubmitting={addAddress.isPending || updateAddress.isPending}
        onSubmit={handleAddressSubmit}
      />

      {/* ── Confirm Delete Customer ────────────────────────────────────── */}
      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Delete Customer?"
        description={`Are you sure you want to delete "${customer.fullName}"? This cannot be undone.`}
        confirmLabel="Delete Customer"
        variant="destructive"
        loading={deleteCustomer.isPending}
        onConfirm={handleDelete}
      />

      {/* ── Confirm Delete Address ─────────────────────────────────────── */}
      <ConfirmDialog
        open={!!deletingAddr}
        onOpenChange={(o) => !o && setDeletingAddr(null)}
        title="Delete Address?"
        description="This address will be permanently removed."
        confirmLabel="Delete Address"
        variant="destructive"
        loading={deleteAddress.isPending}
        onConfirm={handleDeleteAddress}
      />
    </div>
  )
}
