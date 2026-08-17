/**
 * features/orders/pages/CreateOrderPage.tsx
 * Full-page order creation with 5-step wizard.
 * Pattern mirrors CreateCustomerPage.tsx.
 */
import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { CreateOrderForm } from '../components/CreateOrderForm'
import { useCreateOrder } from '../hooks/useOrders'
import type { CreateOrderRequest } from '@/types/order.types'

export function CreateOrderPage() {
  const navigate = useNavigate()
  const create   = useCreateOrder()

  const handleSubmit = (req: CreateOrderRequest) => {
    create.mutate(req, {
      onSuccess: (res) => {
        const id = res.data?.id
        if (id) navigate(`/orders/${id}`)
        else navigate('/orders')
      },
    })
  }

  return (
    <div className="animate-fade-in max-w-3xl mx-auto space-y-6">

      {/* Header */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost" size="icon"
          onClick={() => navigate('/orders')}
          style={{ color: 'var(--text-muted)' }}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-heading-1" style={{ color: 'var(--text-primary)' }}>
            New Order
          </h1>
          <p className="text-body-sm mt-0.5" style={{ color: 'var(--text-secondary)' }}>
            Complete each step to create a new customer order.
          </p>
        </div>
      </div>

      {/* Form card */}
      <div
        className="p-6 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <CreateOrderForm
          isSubmitting={create.isPending}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/orders')}
        />
      </div>
    </div>
  )
}
