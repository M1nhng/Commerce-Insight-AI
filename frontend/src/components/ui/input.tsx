/**
 * components/ui/input.tsx
 */
import * as React from 'react'
import { cn } from '@/lib/utils'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, ...props }, ref) => (
    <input
      type={type}
      ref={ref}
      className={cn(
        'flex h-9 w-full rounded-lg border px-3 py-2 text-body-sm',
        'placeholder:text-[var(--text-muted)]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent-500)] focus-visible:ring-offset-0',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'transition-colors duration-150',
        className
      )}
      {...props}
    />
  )
)
Input.displayName = 'Input'
