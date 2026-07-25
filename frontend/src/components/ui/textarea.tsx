/**
 * components/ui/textarea.tsx
 */
import * as React from 'react'
import { cn } from '@/lib/utils'

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {}

export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        'flex min-h-[80px] w-full rounded-lg border px-3 py-2 text-body-sm',
        'placeholder:text-[var(--text-muted)]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent-500)]',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'transition-colors duration-150',
        className
      )}
      {...props}
    />
  )
)
Textarea.displayName = 'Textarea'
