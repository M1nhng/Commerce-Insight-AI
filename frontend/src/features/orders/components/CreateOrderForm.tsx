/**
 * features/orders/components/CreateOrderForm.tsx
 *
 * 5-step order creation wizard:
 *   1. Select Customer
 *   2. Add Products (line items)
 *   3. Addresses (optional)
 *   4. Payment & Notes
 *   5. Review & Submit
 *
 * Architecture:
 * - State is held locally (no global store needed)
 * - Backend calculates totals — frontend NEVER sends computed amounts
 * - Step 5 (Review) is readonly — user sees what they selected, not computed totals
 */
import { useState, useCallback } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search, Plus, Trash2, ChevronRight, ChevronLeft, Loader2, Package, User2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type { CustomerSummaryResponse } from '@/types/customer.types'
import type {
  CreateOrderRequest,
  CreateOrderItemRequest,
  CreateOrderAddressRequest,
  PaymentMethod,
} from '@/types/order.types'
import { PAYMENT_METHOD_LABELS } from '@/types/order.types'

// ── Step indicator ────────────────────────────────────────────────────────

const STEPS = ['Customer', 'Products', 'Addresses', 'Payment', 'Review']

function StepIndicator({ current }: { current: number }) {
  return (
    <div className="flex items-center gap-0 mb-8 overflow-x-auto pb-1">
      {STEPS.map((label, idx) => {
        const done    = idx < current
        const active  = idx === current
        return (
          <div key={label} className="flex items-center shrink-0">
            <div className="flex flex-col items-center gap-1">
              <div
                className="w-8 h-8 rounded-full flex items-center justify-center text-caption font-semibold transition-all"
                style={{
                  background: done ? 'var(--success)' : active ? 'var(--accent-500)' : 'var(--bg-overlay)',
                  color: done || active ? '#fff' : 'var(--text-muted)',
                  border: `2px solid ${done ? 'var(--success)' : active ? 'var(--accent-500)' : 'var(--border-default)'}`,
                }}
              >
                {done ? '✓' : idx + 1}
              </div>
              <span
                className="text-caption whitespace-nowrap"
                style={{ color: active ? 'var(--text-primary)' : 'var(--text-muted)' }}
              >
                {label}
              </span>
            </div>
            {idx < STEPS.length - 1 && (
              <div
                className="h-px w-8 sm:w-12 mx-1 mb-5"
                style={{ background: done ? 'var(--success)' : 'var(--border-default)' }}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}

// ── Address sub-form ──────────────────────────────────────────────────────

interface AddressFormState {
  recipientName: string
  phone: string
  addressLine: string
  ward: string
  district: string
  province: string
  country: string
}

const EMPTY_ADDRESS: AddressFormState = {
  recipientName: '', phone: '', addressLine: '', ward: '', district: '', province: '', country: 'Vietnam',
}

function AddressSubForm({
  title,
  value,
  onChange,
}: {
  title: string
  value: AddressFormState
  onChange: (v: AddressFormState) => void
}) {
  const field = (key: keyof AddressFormState, label: string, required = false) => (
    <div className="space-y-1">
      <label className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
        {label}{required && <span style={{ color: 'var(--error)' }}> *</span>}
      </label>
      <Input
        value={value[key]}
        onChange={(e) => onChange({ ...value, [key]: e.target.value })}
        style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
      />
    </div>
  )

  return (
    <div
      className="rounded-xl border p-4 space-y-3"
      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
    >
      <h4 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{title}</h4>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {field('recipientName', 'Recipient Name', true)}
        {field('phone', 'Phone')}
        <div className="sm:col-span-2">{field('addressLine', 'Address Line', true)}</div>
        {field('ward', 'Ward / Commune')}
        {field('district', 'District')}
        {field('province', 'Province / City')}
        {field('country', 'Country')}
      </div>
    </div>
  )
}

// ── Main Form ─────────────────────────────────────────────────────────────

export interface CreateOrderFormValues {
  customerId: string
  items: Array<{ productId: string; productName: string; sku: string; price: number; quantity: number; discount: number }>
  shippingEnabled: boolean
  shippingAddress: AddressFormState
  billingEnabled: boolean
  billingAddress: AddressFormState
  paymentMethod: PaymentMethod
  shippingFee: number
  notes: string
}

interface CreateOrderFormProps {
  isSubmitting: boolean
  onSubmit: (req: CreateOrderRequest) => void
  onCancel: () => void
}

export function CreateOrderForm({ isSubmitting, onSubmit, onCancel }: CreateOrderFormProps) {
  const [step, setStep] = useState(0)

  // Step 1 state
  const [customerSearch, setCustomerSearch] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerSummaryResponse | null>(null)

  // Step 2 state
  const [productSearch, setProductSearch] = useState('')
  const [lineItems, setLineItems] = useState<CreateOrderFormValues['items']>([])

  // Step 3 state
  const [shippingEnabled, setShippingEnabled] = useState(false)
  const [shippingAddress, setShippingAddress] = useState<AddressFormState>(EMPTY_ADDRESS)
  const [billingEnabled, setBillingEnabled] = useState(false)
  const [billingAddress, setBillingAddress] = useState<AddressFormState>(EMPTY_ADDRESS)

  // Step 4 state
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH')
  const [shippingFee, setShippingFee] = useState(0)
  const [notes, setNotes] = useState('')

  // ── Customer search query ──────────────────────────────────────────────
  const { data: customerData, isFetching: searchingCustomers } = useQuery({
    queryKey: ['customer-search', customerSearch],
    queryFn: () =>
      apiClient
        .get<ApiResponse<PageResponse<CustomerSummaryResponse>>>('/customers', {
          params: { keyword: customerSearch, size: 8, status: 'ACTIVE' },
        })
        .then((r) => r.data.data?.content ?? []),
    enabled: customerSearch.length > 0,
    staleTime: 10_000,
  })

  // ── Product search query ───────────────────────────────────────────────
  const { data: productData, isFetching: searchingProducts } = useQuery({
    queryKey: ['product-search-order', productSearch],
    queryFn: () =>
      apiClient
        .get<ApiResponse<PageResponse<any>>>('/products', {
          params: { keyword: productSearch, size: 8, active: true },
        })
        .then((r) => r.data.data?.content ?? []),
    enabled: productSearch.length > 0,
    staleTime: 10_000,
  })

  // ── Line item helpers ──────────────────────────────────────────────────
  const addProduct = useCallback((product: any) => {
    setLineItems((prev) => {
      const exists = prev.find((i) => i.productId === product.id)
      if (exists) return prev.map((i) => i.productId === product.id ? { ...i, quantity: i.quantity + 1 } : i)
      return [...prev, {
        productId:   product.id,
        productName: product.name,
        sku:         product.sku,
        price:       product.price,
        quantity:    1,
        discount:    0,
      }]
    })
    setProductSearch('')
  }, [])

  const updateItem = (productId: string, field: 'quantity' | 'discount', value: number) => {
    setLineItems((prev) =>
      prev.map((i) => i.productId === productId ? { ...i, [field]: Math.max(0, value) } : i)
    )
  }

  const removeItem = (productId: string) => {
    setLineItems((prev) => prev.filter((i) => i.productId !== productId))
  }

  // ── Validation per step ────────────────────────────────────────────────
  const canNext = () => {
    if (step === 0) return !!selectedCustomer
    if (step === 1) return lineItems.length > 0 && lineItems.every((i) => i.quantity > 0)
    if (step === 2) {
      if (shippingEnabled && (!shippingAddress.recipientName || !shippingAddress.addressLine)) return false
      if (billingEnabled && (!billingAddress.recipientName || !billingAddress.addressLine)) return false
      return true
    }
    return true
  }

  // ── Build final request (step 5 submit) ───────────────────────────────
  const handleSubmit = () => {
    if (!selectedCustomer) return
    const toAddr = (enabled: boolean, addr: AddressFormState, type: 'SHIPPING' | 'BILLING'): CreateOrderAddressRequest | null => {
      if (!enabled) return null
      return { type, recipientName: addr.recipientName, phone: addr.phone || null,
        addressLine: addr.addressLine, ward: addr.ward || null,
        district: addr.district || null, province: addr.province || null,
        country: addr.country || 'Vietnam' }
    }

    const req: CreateOrderRequest = {
      customerId:      selectedCustomer.id,
      items:           lineItems.map((i): CreateOrderItemRequest => ({
        productId:     i.productId,
        quantity:      i.quantity,
        discountAmount: i.discount > 0 ? i.discount : null,
      })),
      shippingAddress: toAddr(shippingEnabled, shippingAddress, 'SHIPPING'),
      billingAddress:  toAddr(billingEnabled, billingAddress, 'BILLING'),
      paymentMethod,
      shippingFee:     shippingFee > 0 ? shippingFee : null,
      notes:           notes || null,
    }
    onSubmit(req)
  }

  // ── Render ─────────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      <StepIndicator current={step} />

      {/* STEP 1: Select Customer */}
      {step === 0 && (
        <div className="space-y-4">
          <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>Select Customer</h2>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4" style={{ color: 'var(--text-muted)' }} />
            <Input
              value={customerSearch}
              onChange={(e) => setCustomerSearch(e.target.value)}
              placeholder="Search by name, code, or email..."
              className="pl-9"
              style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
            />
          </div>

          {searchingCustomers && (
            <div className="flex items-center gap-2 py-4" style={{ color: 'var(--text-muted)' }}>
              <Loader2 className="h-4 w-4 animate-spin" /> Searching…
            </div>
          )}

          {customerData && customerData.length > 0 && (
            <div className="space-y-2">
              {customerData.map((c) => (
                <button
                  key={c.id}
                  onClick={() => { setSelectedCustomer(c); setCustomerSearch('') }}
                  className="w-full text-left p-3 rounded-lg border transition-colors"
                  style={{
                    background: selectedCustomer?.id === c.id ? 'var(--bg-overlay)' : 'var(--bg-elevated)',
                    borderColor: selectedCustomer?.id === c.id ? 'var(--accent-500)' : 'var(--border-default)',
                  }}
                >
                  <div className="flex items-center gap-3">
                    <User2 className="h-4 w-4 shrink-0" style={{ color: 'var(--accent-400)' }} />
                    <div>
                      <p className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>{c.fullName}</p>
                      <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
                        {c.customerCode} {c.email ? `· ${c.email}` : ''} {c.phone ? `· ${c.phone}` : ''}
                      </p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}

          {selectedCustomer && (
            <div
              className="flex items-center gap-3 p-4 rounded-xl border"
              style={{ background: 'var(--bg-surface)', borderColor: 'var(--accent-500)' }}
            >
              <User2 className="h-5 w-5 shrink-0" style={{ color: 'var(--accent-400)' }} />
              <div className="flex-1">
                <p className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
                  {selectedCustomer.fullName}
                </p>
                <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
                  {selectedCustomer.customerCode} {selectedCustomer.email ? `· ${selectedCustomer.email}` : ''}
                </p>
              </div>
              <Button variant="ghost" size="sm" onClick={() => setSelectedCustomer(null)} style={{ color: 'var(--text-muted)' }}>
                Change
              </Button>
            </div>
          )}
        </div>
      )}

      {/* STEP 2: Add Products */}
      {step === 1 && (
        <div className="space-y-4">
          <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>Add Products</h2>

          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4" style={{ color: 'var(--text-muted)' }} />
            <Input
              value={productSearch}
              onChange={(e) => setProductSearch(e.target.value)}
              placeholder="Search product by name or SKU..."
              className="pl-9"
              style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
            />
          </div>

          {searchingProducts && (
            <div className="flex items-center gap-2 py-2" style={{ color: 'var(--text-muted)' }}>
              <Loader2 className="h-4 w-4 animate-spin" /> Searching…
            </div>
          )}

          {productData && productData.length > 0 && (
            <div
              className="border rounded-lg overflow-hidden"
              style={{ borderColor: 'var(--border-default)' }}
            >
              {productData.map((p: any) => (
                <button
                  key={p.id}
                  onClick={() => addProduct(p)}
                  className="w-full text-left px-4 py-3 flex items-center gap-3 hover:bg-[var(--bg-overlay)] transition-colors border-b last:border-0"
                  style={{ borderColor: 'var(--border-subtle)', background: 'var(--bg-elevated)' }}
                >
                  <Package className="h-4 w-4 shrink-0" style={{ color: 'var(--accent-400)' }} />
                  <div className="flex-1 min-w-0">
                    <p className="text-body-sm font-medium truncate" style={{ color: 'var(--text-primary)' }}>{p.name}</p>
                    <p className="text-caption" style={{ color: 'var(--text-muted)' }}>{p.sku}</p>
                  </div>
                  <span className="text-body-sm font-semibold shrink-0" style={{ color: 'var(--accent-400)' }}>
                    {new Intl.NumberFormat('vi-VN').format(p.price)}
                  </span>
                  <Plus className="h-4 w-4 shrink-0" style={{ color: 'var(--text-muted)' }} />
                </button>
              ))}
            </div>
          )}

          {/* Line items list */}
          {lineItems.length > 0 ? (
            <div
              className="rounded-xl border overflow-hidden"
              style={{ borderColor: 'var(--border-default)' }}
            >
              <table className="w-full text-body-sm">
                <thead>
                  <tr style={{ background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-subtle)' }}>
                    {['Product', 'Unit Price', 'Qty', 'Discount', ''].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-caption font-medium uppercase tracking-wide" style={{ color: 'var(--text-muted)' }}>
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {lineItems.map((item) => (
                    <tr key={item.productId} style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-surface)' }}>
                      <td className="px-4 py-3">
                        <p className="font-medium" style={{ color: 'var(--text-primary)' }}>{item.productName}</p>
                        <p className="text-caption font-mono" style={{ color: 'var(--accent-400)' }}>{item.sku}</p>
                      </td>
                      <td className="px-4 py-3" style={{ color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                        {new Intl.NumberFormat('vi-VN').format(item.price)}
                      </td>
                      <td className="px-4 py-3">
                        <Input
                          type="number" min={1} value={item.quantity}
                          onChange={(e) => updateItem(item.productId, 'quantity', parseInt(e.target.value) || 1)}
                          className="w-20 h-8 text-center"
                          style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <Input
                          type="number" min={0} value={item.discount}
                          onChange={(e) => updateItem(item.productId, 'discount', parseFloat(e.target.value) || 0)}
                          className="w-28 h-8"
                          style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => removeItem(item.productId)} style={{ color: 'var(--error)' }}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div
              className="flex flex-col items-center justify-center py-12 rounded-xl border border-dashed"
              style={{ borderColor: 'var(--border-default)' }}
            >
              <Package className="h-10 w-10 mb-3" style={{ color: 'var(--text-muted)' }} />
              <p className="text-body-sm" style={{ color: 'var(--text-muted)' }}>Search and add products above</p>
            </div>
          )}
        </div>
      )}

      {/* STEP 3: Addresses */}
      {step === 2 && (
        <div className="space-y-6">
          <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>Addresses <span className="text-body-sm font-normal" style={{ color: 'var(--text-muted)' }}>(optional)</span></h2>

          {/* Shipping */}
          <div className="space-y-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={shippingEnabled}
                onChange={(e) => setShippingEnabled(e.target.checked)}
                className="rounded"
                style={{ accentColor: 'var(--accent-500)' }}
              />
              <span className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>Add Shipping Address</span>
            </label>
            {shippingEnabled && (
              <AddressSubForm title="Shipping Address" value={shippingAddress} onChange={setShippingAddress} />
            )}
          </div>

          {/* Billing */}
          <div className="space-y-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={billingEnabled}
                onChange={(e) => setBillingEnabled(e.target.checked)}
                className="rounded"
                style={{ accentColor: 'var(--accent-500)' }}
              />
              <span className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>Add Billing Address</span>
            </label>
            {billingEnabled && (
              <AddressSubForm title="Billing Address" value={billingAddress} onChange={setBillingAddress} />
            )}
          </div>

          {!shippingEnabled && !billingEnabled && (
            <p className="text-body-sm" style={{ color: 'var(--text-muted)' }}>
              Check the boxes above to add addresses. Both fields are optional.
            </p>
          )}
        </div>
      )}

      {/* STEP 4: Payment & Notes */}
      {step === 3 && (
        <div className="space-y-6">
          <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>Payment & Notes</h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Payment Method */}
            <div className="space-y-1">
              <label className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
                Payment Method <span style={{ color: 'var(--error)' }}>*</span>
              </label>
              <Select value={paymentMethod} onValueChange={(v) => setPaymentMethod(v as PaymentMethod)}>
                <SelectTrigger style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                  {(Object.entries(PAYMENT_METHOD_LABELS) as [PaymentMethod, string][]).map(([k, v]) => (
                    <SelectItem key={k} value={k} style={{ color: 'var(--text-primary)' }}>{v}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Shipping Fee */}
            <div className="space-y-1">
              <label className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>Shipping Fee (VND)</label>
              <Input
                type="number" min={0} value={shippingFee}
                onChange={(e) => setShippingFee(parseFloat(e.target.value) || 0)}
                style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
              />
            </div>

            {/* Notes */}
            <div className="space-y-1 sm:col-span-2">
              <label className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>Notes</label>
              <textarea
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Optional order notes..."
                className="w-full rounded-md border px-3 py-2 text-body-sm resize-none"
                style={{
                  background: 'var(--bg-elevated)',
                  borderColor: 'var(--border-default)',
                  color: 'var(--text-primary)',
                  outline: 'none',
                }}
              />
            </div>
          </div>

          <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
            💡 Order totals are calculated by the server when you submit. The Review step shows your selections.
          </p>
        </div>
      )}

      {/* STEP 5: Review */}
      {step === 4 && (
        <div className="space-y-6">
          <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>Review Order</h2>

          {/* Customer */}
          <div className="rounded-xl border p-4" style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}>
            <p className="text-caption font-medium uppercase tracking-wide mb-2" style={{ color: 'var(--text-muted)' }}>Customer</p>
            <p className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{selectedCustomer?.fullName}</p>
            <p className="text-caption" style={{ color: 'var(--text-muted)' }}>{selectedCustomer?.customerCode} · {selectedCustomer?.email}</p>
          </div>

          {/* Items */}
          <div className="rounded-xl border overflow-hidden" style={{ borderColor: 'var(--border-default)' }}>
            <table className="w-full text-body-sm">
              <thead>
                <tr style={{ background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-subtle)' }}>
                  {['Product', 'Qty', 'Unit Price', 'Discount'].map((h) => (
                    <th key={h} className="px-4 py-2 text-left text-caption font-medium uppercase tracking-wide" style={{ color: 'var(--text-muted)' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {lineItems.map((item) => (
                  <tr key={item.productId} style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-surface)' }}>
                    <td className="px-4 py-2" style={{ color: 'var(--text-primary)' }}>{item.productName}</td>
                    <td className="px-4 py-2 text-center" style={{ color: 'var(--text-secondary)' }}>{item.quantity}</td>
                    <td className="px-4 py-2" style={{ color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                      {new Intl.NumberFormat('vi-VN').format(item.price)}
                    </td>
                    <td className="px-4 py-2" style={{ color: item.discount > 0 ? 'var(--warning)' : 'var(--text-muted)' }}>
                      {item.discount > 0 ? `- ${new Intl.NumberFormat('vi-VN').format(item.discount)}` : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Payment */}
          <div className="rounded-xl border p-4 grid grid-cols-2 gap-3" style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}>
            <div>
              <p className="text-caption" style={{ color: 'var(--text-muted)' }}>Payment Method</p>
              <p className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>{PAYMENT_METHOD_LABELS[paymentMethod]}</p>
            </div>
            <div>
              <p className="text-caption" style={{ color: 'var(--text-muted)' }}>Shipping Fee</p>
              <p className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                {shippingFee > 0 ? new Intl.NumberFormat('vi-VN').format(shippingFee) + ' VND' : 'Free'}
              </p>
            </div>
            {notes && (
              <div className="col-span-2">
                <p className="text-caption" style={{ color: 'var(--text-muted)' }}>Notes</p>
                <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>{notes}</p>
              </div>
            )}
          </div>

          <div
            className="rounded-lg border p-3 text-body-sm"
            style={{ background: 'var(--info-bg)', borderColor: 'var(--info)', color: 'var(--info)' }}
          >
            ℹ️ Exact totals (subtotal, tax, grand total) are calculated by the server upon submission.
          </div>
        </div>
      )}

      {/* Navigation Buttons */}
      <div className="flex items-center justify-between pt-4 border-t" style={{ borderColor: 'var(--border-subtle)' }}>
        <Button
          variant="outline"
          onClick={step === 0 ? onCancel : () => setStep((s) => s - 1)}
          style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
        >
          <ChevronLeft className="h-4 w-4 mr-1" />
          {step === 0 ? 'Cancel' : 'Back'}
        </Button>

        {step < 4 ? (
          <Button
            onClick={() => setStep((s) => s + 1)}
            disabled={!canNext()}
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            Next
            <ChevronRight className="h-4 w-4 ml-1" />
          </Button>
        ) : (
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting}
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            {isSubmitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
            Create Order
          </Button>
        )}
      </div>
    </div>
  )
}
