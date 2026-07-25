/**
 * features/products/components/CategoryForm.tsx
 *
 * Create / Edit category form using React Hook Form + Zod.
 * Renders inside a Dialog modal.
 */
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { useCategoryOptions } from '../hooks/useCategories'
import { useCreateCategory, useUpdateCategory } from '../hooks/useCategories'
import type { CategoryResponse } from '@/types/product.types'

const categorySchema = z.object({
  name: z.string().min(1, 'Name is required').max(150, 'Max 150 chars'),
  description: z.string().max(1000, 'Max 1000 chars').optional().nullable(),
  parentId: z.string().uuid().optional().nullable(),
  sortOrder: z.coerce.number().int().min(0).max(9999).optional(),
  active: z.boolean().optional(),
})

type CategoryFormValues = z.infer<typeof categorySchema>

interface CategoryFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  category?: CategoryResponse | null
}

export function CategoryForm({ open, onOpenChange, category }: CategoryFormProps) {
  const isEdit = !!category
  const { data: categoryOptions = [] } = useCategoryOptions()
  const createMutation = useCreateCategory()
  const updateMutation = useUpdateCategory()
  const isPending = createMutation.isPending || updateMutation.isPending

  const form = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      name: '',
      description: '',
      parentId: null,
      sortOrder: 0,
      active: true,
    },
  })

  useEffect(() => {
    if (category) {
      form.reset({
        name: category.name,
        description: category.description ?? '',
        parentId: category.parentId ?? null,
        sortOrder: category.sortOrder,
        active: category.active,
      })
    } else {
      form.reset({ name: '', description: '', parentId: null, sortOrder: 0, active: true })
    }
  }, [category, form])

  // Filter out the current category from parent options (prevent self-reference)
  const parentOptions = categoryOptions.filter((o) => o.value !== category?.id)

  const onSubmit = async (values: CategoryFormValues) => {
    const payload = {
      name: values.name,
      description: values.description || null,
      parentId: values.parentId || null,
      sortOrder: values.sortOrder ?? 0,
    }
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({
          id: category!.id,
          data: { ...payload, active: values.active ?? true },
        })
      } else {
        await createMutation.mutateAsync(payload)
      }
      onOpenChange(false)
    } catch {
      // handled by mutation
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="sm:max-w-[480px]"
        style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
      >
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)' }}>
            {isEdit ? 'Edit Category' : 'New Category'}
          </DialogTitle>
          <DialogDescription style={{ color: 'var(--text-secondary)' }}>
            {isEdit ? 'Update category details.' : 'Create a new product category.'}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 mt-2">
            {/* Name */}
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>
                    Name <span style={{ color: 'var(--error)' }}>*</span>
                  </FormLabel>
                  <FormControl>
                    <Input
                      {...field}
                      placeholder="e.g. Electronics"
                      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Parent */}
            <FormField
              control={form.control}
              name="parentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>Parent Category</FormLabel>
                  <Select
                    value={field.value ?? '__none__'}
                    onValueChange={(v) => field.onChange(v === '__none__' ? null : v)}
                  >
                    <FormControl>
                      <SelectTrigger
                        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                      >
                        <SelectValue placeholder="Root category (no parent)" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                      <SelectItem value="__none__" style={{ color: 'var(--text-muted)' }}>
                        Root (no parent)
                      </SelectItem>
                      {parentOptions.map((opt) => (
                        <SelectItem key={opt.value} value={opt.value} style={{ color: 'var(--text-primary)' }}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Description */}
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>Description</FormLabel>
                  <FormControl>
                    <Textarea
                      {...field}
                      value={field.value ?? ''}
                      rows={2}
                      placeholder="Optional description..."
                      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)', color: 'var(--text-primary)', resize: 'vertical' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Sort order */}
            <FormField
              control={form.control}
              name="sortOrder"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>Sort Order</FormLabel>
                  <FormControl>
                    <Input
                      {...field}
                      type="number"
                      min="0"
                      placeholder="0"
                      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Active toggle (edit only) */}
            {isEdit && (
              <FormField
                control={form.control}
                name="active"
                render={({ field }) => (
                  <FormItem className="flex items-center gap-3">
                    <FormControl>
                      <Switch checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                    <FormLabel className="cursor-pointer" style={{ color: 'var(--text-secondary)' }}>
                      Active
                    </FormLabel>
                  </FormItem>
                )}
              />
            )}

            {/* Actions */}
            <div className="flex gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                className="flex-1"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
                style={{ background: 'var(--bg-overlay)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                className="flex-1"
                disabled={isPending}
                style={{ background: 'var(--accent-500)', color: '#fff' }}
              >
                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isEdit ? 'Save Changes' : 'Create Category'}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
