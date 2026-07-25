/**
 * components/common/ConfirmDialog.tsx
 * Reusable delete/action confirmation dialog using shadcn/ui AlertDialog.
 */
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Loader2 } from 'lucide-react'

interface ConfirmDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title?: string
  description?: string
  confirmLabel?: string
  variant?: 'destructive' | 'default'
  loading?: boolean
  onConfirm: () => void
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title = 'Are you sure?',
  description = 'This action cannot be undone.',
  confirmLabel = 'Confirm',
  variant = 'destructive',
  loading = false,
  onConfirm,
}: ConfirmDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent
        style={{
          background: 'var(--bg-elevated)',
          border: '1px solid var(--border-default)',
        }}
      >
        <AlertDialogHeader>
          <AlertDialogTitle style={{ color: 'var(--text-primary)' }}>
            {title}
          </AlertDialogTitle>
          <AlertDialogDescription style={{ color: 'var(--text-secondary)' }}>
            {description}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            disabled={loading}
            style={{
              background: 'var(--bg-overlay)',
              border: '1px solid var(--border-default)',
              color: 'var(--text-secondary)',
            }}
          >
            Cancel
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={loading}
            style={
              variant === 'destructive'
                ? { background: 'var(--error)', color: '#fff' }
                : { background: 'var(--accent-500)', color: '#fff' }
            }
          >
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
