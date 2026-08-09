/**
 * features/customers/pages/EditCustomerPage.tsx
 * Full-page customer edit form — pre-fills existing data.
 */
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { CardSkeleton } from '@/components/common/TableSkeleton'
import { CustomerForm, type CustomerFormValues } from '../components/CustomerForm'
import { useCustomer, useUpdateCustomer } from '../hooks/useCustomers'

export function EditCustomerPage() {
  const { id }    = useParams<{ id: string }>()
  const navigate  = useNavigate()
  const { data: customer, isLoading } = useCustomer(id ?? null)
  const update    = useUpdateCustomer()

  const handleSubmit = (values: CustomerFormValues) => {
    if (!id) return
    update.mutate(
      {
        id,
        data: {
          firstName:   values.firstName,
          lastName:    values.lastName,
          email:       values.email  || undefined,
          phone:       values.phone  || undefined,
          dateOfBirth: values.dateOfBirth || undefined,
          gender:      (values.gender === '__none__' ? undefined : values.gender) as any,
          groupId:     values.groupId === '__none__' ? undefined : values.groupId,
        },
      },
      { onSuccess: () => navigate(`/customers/${id}`) }
    )
  }

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
        <CardSkeleton className="h-16" />
        <CardSkeleton className="h-80" />
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
    <div className="animate-fade-in max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate(-1)}
          style={{ color: 'var(--text-muted)' }}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-heading-1" style={{ color: 'var(--text-primary)' }}>
            Edit Customer
          </h1>
          <p className="text-body-sm mt-0.5 font-mono" style={{ color: 'var(--accent-400)' }}>
            {customer.customerCode}
          </p>
        </div>
      </div>

      {/* Form card */}
      <div
        className="p-6 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <CustomerForm
          customer={customer}
          isSubmitting={update.isPending}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/customers/${id}`)}
        />
      </div>
    </div>
  )
}
