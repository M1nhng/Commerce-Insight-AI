/**
 * features/products/components/ProductForm.tsx
 *
 * Create / Edit product form using React Hook Form + Zod.
 * Renders inside a Sheet (side drawer) per design spec §12.3.
 */
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet'
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
import { useCreateProduct, useUpdateProduct } from '../hooks/useProducts'
import type { ProductResponse } from '@/types/product.types'

// ── Zod schema ────────────────────────────────────────────────────────────

const productSchema = z.object({
  sku: z.string().min(1, 'SKU is required').max(100, 'Max 100 chars'),
  name: z.string().min(1, 'Name is required').max(255, 'Max 255 chars'),
  description: z.string().max(5000, 'Max 5000 chars').optional().nullable(),
  price: z
    .string()
    .min(1, 'Price is required')
    .refine((v) => !isNaN(Number(v)) && Number(v) >= 0, 'Must be 0 or greater'),
  costPrice: z
    .string()
    .optional()
    .nullable()
    .refine((v) => !v || (!isNaN(Number(v)) && Number(v) >= 0), 'Must be 0 or greater'),
  imageUrl: z.string().url('Must be a valid URL').optional().nullable().or(z.literal('')),
  categoryId: z.string().uuid().optional().nullable(),
  initialStock: z.coerce.number().int().min(0).optional(),
  active: z.boolean().optional(),
})

type ProductFormValues = z.infer<typeof productSchema>

// ── Props ─────────────────────────────────────────────────────────────────

interface ProductFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When provided, renders in edit mode */
  product?: ProductResponse | null
}

// ── Component ─────────────────────────────────────────────────────────────

export function ProductForm({ open, onOpenChange, product }: ProductFormProps) {
  const isEdit = !!product
  const { data: categoryOptions = [] } = useCategoryOptions()
  const createMutation = useCreateProduct()
  const updateMutation = useUpdateProduct()

  const isPending = createMutation.isPending || updateMutation.isPending

  const form = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      sku: '',
      name: '',
      description: '',
      price: '',
      costPrice: '',
      imageUrl: '',
      categoryId: null,
      initialStock: 0,
      active: true,
    },
  })

  // Populate form when editing
  useEffect(() => {
    if (product) {
      form.reset({
        sku: product.sku,
        name: product.name,
        description: product.description ?? '',
        price: String(product.price),
        costPrice: product.costPrice != null ? String(product.costPrice) : '',
        imageUrl: product.imageUrl ?? '',
        categoryId: product.categoryId ?? null,
        active: product.active,
      })
    } else {
      form.reset({
        sku: '',
        name: '',
        description: '',
        price: '',
        costPrice: '',
        imageUrl: '',
        categoryId: null,
        initialStock: 0,
        active: true,
      })
    }
  }, [product, form])

  const onSubmit = async (values: ProductFormValues) => {
    const payload = {
      sku: values.sku,
      name: values.name,
      description: values.description || null,
      price: Number(values.price),
      costPrice: values.costPrice ? Number(values.costPrice) : null,
      imageUrl: values.imageUrl || null,
      categoryId: values.categoryId || null,
    }

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({
          id: product!.id,
          data: { ...payload, active: values.active ?? true },
        })
      } else {
        await createMutation.mutateAsync({
          ...payload,
          initialStock: values.initialStock ?? 0,
        })
      }
      onOpenChange(false)
    } catch {
      // Error handled by mutation onError
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="w-full sm:max-w-[560px] overflow-y-auto"
        style={{
          background: 'var(--bg-surface)',
          borderLeft: '1px solid var(--border-default)',
        }}
      >
        <SheetHeader className="mb-6">
          <SheetTitle style={{ color: 'var(--text-primary)' }}>
            {isEdit ? 'Edit Product' : 'Add Product'}
          </SheetTitle>
          <SheetDescription style={{ color: 'var(--text-secondary)' }}>
            {isEdit
              ? 'Update the product details below.'
              : 'Fill in the details to add a new product to your catalog.'}
          </SheetDescription>
        </SheetHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
            {/* SKU */}
            <FormField
              control={form.control}
              name="sku"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>
                    SKU <span style={{ color: 'var(--error)' }}>*</span>
                  </FormLabel>
                  <FormControl>
                    <Input
                      {...field}
                      placeholder="e.g. SKU-001"
                      className="font-mono uppercase"
                      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Name */}
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>
                    Product Name <span style={{ color: 'var(--error)' }}>*</span>
                  </FormLabel>
                  <FormControl>
                    <Input
                      {...field}
                      placeholder="e.g. Wireless Headphones Pro"
                      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Price & Cost row */}
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="price"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel style={{ color: 'var(--text-secondary)' }}>
                      Selling Price <span style={{ color: 'var(--error)' }}>*</span>
                    </FormLabel>
                    <FormControl>
                      <div className="relative">
                        <span
                          className="absolute left-3 top-1/2 -translate-y-1/2 text-body-sm"
                          style={{ color: 'var(--text-muted)' }}
                        >$</span>
                        <Input
                          {...field}
                          type="number"
                          min="0"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-7"
                          style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                        />
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="costPrice"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel style={{ color: 'var(--text-secondary)' }}>
                      Cost Price
                    </FormLabel>
                    <FormControl>
                      <div className="relative">
                        <span
                          className="absolute left-3 top-1/2 -translate-y-1/2 text-body-sm"
                          style={{ color: 'var(--text-muted)' }}
                        >$</span>
                        <Input
                          {...field}
                          value={field.value ?? ''}
                          type="number"
                          min="0"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-7"
                          style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                        />
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Category */}
            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>Category</FormLabel>
                  <Select
                    value={field.value ?? '__none__'}
                    onValueChange={(v) => field.onChange(v === '__none__' ? null : v)}
                  >
                    <FormControl>
                      <SelectTrigger
                        style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                      >
                        <SelectValue placeholder="Select a category..." />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                      <SelectItem value="__none__" style={{ color: 'var(--text-muted)' }}>
                        No category
                      </SelectItem>
                      {categoryOptions.map((opt) => (
                        <SelectItem
                          key={opt.value}
                          value={opt.value}
                          style={{ color: 'var(--text-primary)' }}
                        >
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
                      rows={3}
                      placeholder="Optional product description..."
                      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)', resize: 'vertical' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Image URL */}
            <FormField
              control={form.control}
              name="imageUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel style={{ color: 'var(--text-secondary)' }}>Image URL</FormLabel>
                  <FormControl>
                    <Input
                      {...field}
                      value={field.value ?? ''}
                      type="url"
                      placeholder="https://..."
                      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Initial stock (create only) */}
            {!isEdit && (
              <FormField
                control={form.control}
                name="initialStock"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel style={{ color: 'var(--text-secondary)' }}>Initial Stock</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        type="number"
                        min="0"
                        placeholder="0"
                        style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

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
                      Active (visible in catalog)
                    </FormLabel>
                  </FormItem>
                )}
              />
            )}

            {/* Actions */}
            <div className="flex gap-3 pt-4 border-t" style={{ borderColor: 'var(--border-subtle)' }}>
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
                {isEdit ? 'Save Changes' : 'Create Product'}
              </Button>
            </div>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  )
}
