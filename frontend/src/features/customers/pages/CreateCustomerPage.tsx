/**
 * features/customers/pages/CreateCustomerPage.tsx
 * Full-page customer creation form.
 */
import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { CustomerForm, type CustomerFormValues } from '../components/CustomerForm'
import { useCreateCustomer } from '../hooks/useCustomers'

export function CreateCustomerPage() {
  const navigate  = useNavigate()
  const create    = useCreateCustomer()

  const handleSubmit = (values: CustomerFormValues) => {
    create.mutate(
      {
        firstName:    values.firstName,
        lastName:     values.lastName,
        email:        values.email  || undefined,
        phone:        values.phone  || undefined,
        dateOfBirth:  values.dateOfBirth || undefined,
        gender:       (values.gender === '__none__' ? undefined : values.gender) as any,
        groupId:      values.groupId === '__none__' ? undefined : values.groupId,
        customerCode: values.customerCode || undefined,
      },
      {
        onSuccess: (res) => {
          const id = res.data?.id
          if (id) navigate(`/customers/${id}`)
          else navigate('/customers')
        },
      }
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
            Add Customer
          </h1>
          <p className="text-body-sm mt-0.5" style={{ color: 'var(--text-secondary)' }}>
            Fill in the details to create a new customer record.
          </p>
        </div>
      </div>

      {/* Form card */}
      <div
        className="p-6 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <CustomerForm
          isSubmitting={create.isPending}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/customers')}
        />
      </div>
    </div>
  )
}
