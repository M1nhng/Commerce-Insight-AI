/**
 * features/auth/components/LoginForm.tsx
 *
 * React Hook Form + Zod validation.
 * Full accessibility: labels, aria-describedby for errors, aria-live region.
 * Handles loading state, server errors, and shows field-level validation.
 */
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Eye, EyeOff, Loader2, Mail, Lock, AlertCircle } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

// ── Zod Schema ──────────────────────────────────────────────────────────
const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters'),
})

type LoginFormValues = z.infer<typeof loginSchema>

// ── Component ─────────────────────────────────────────────────────────────

export function LoginForm() {
  const { login, isLoading, error, clearError } = useAuth()
  const [showPassword, setShowPassword] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
    setFocus,
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  // Focus email on mount
  useEffect(() => { setFocus('email') }, [setFocus])

  // Clear server error when user starts typing
  useEffect(() => {
    if (error) clearError()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const onSubmit = async (values: LoginFormValues) => {
    try {
      await login(values)
      toast.success('Welcome back!')
    } catch {
      // Error is displayed via the `error` state from useAuth
    }
  }

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-label="Login form"
    >
      {/* ── Server Error Banner ─────────────────────────────────────── */}
      {error && (
        <div
          role="alert"
          aria-live="polite"
          className="mb-5 flex items-start gap-3 rounded-lg px-4 py-3"
          style={{
            background: 'var(--error-bg)',
            border: '1px solid var(--error)',
          }}
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" style={{ color: 'var(--error)' }} />
          <p className="text-body-sm" style={{ color: 'var(--error)' }}>
            {error}
          </p>
        </div>
      )}

      {/* ── Email Field ────────────────────────────────────────────── */}
      <div className="mb-4">
        <label
          htmlFor="email"
          className="mb-1.5 block text-body-sm font-medium"
          style={{ color: 'var(--text-secondary)' }}
        >
          Email address <span style={{ color: 'var(--error)' }} aria-hidden>*</span>
        </label>
        <div className="relative">
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
            <Mail className="h-4 w-4" style={{ color: 'var(--text-muted)' }} />
          </div>
          <input
            id="email"
            type="email"
            autoComplete="email"
            aria-describedby={errors.email ? 'email-error' : undefined}
            aria-invalid={!!errors.email}
            placeholder="you@example.com"
            className={cn(
              'w-full rounded-lg py-2.5 pl-10 pr-4 text-body-md outline-none transition-all duration-200',
              'placeholder:text-[var(--text-muted)]',
              errors.email
                ? 'border-[var(--error)] focus:ring-[var(--error)]/30'
                : 'border-[var(--border-default)] focus:border-[var(--accent-500)] focus:ring-[var(--accent-500)]/20'
            )}
            style={{
              background: 'var(--bg-elevated)',
              border: `1px solid ${errors.email ? 'var(--error)' : 'var(--border-default)'}`,
              color: 'var(--text-primary)',
              boxShadow: errors.email ? '0 0 0 3px var(--error-bg)' : undefined,
            }}
            {...register('email')}
          />
        </div>
        {errors.email && (
          <p
            id="email-error"
            role="alert"
            className="mt-1.5 text-caption"
            style={{ color: 'var(--error)' }}
          >
            {errors.email.message}
          </p>
        )}
      </div>

      {/* ── Password Field ─────────────────────────────────────────── */}
      <div className="mb-6">
        <div className="mb-1.5 flex items-center justify-between">
          <label
            htmlFor="password"
            className="text-body-sm font-medium"
            style={{ color: 'var(--text-secondary)' }}
          >
            Password <span style={{ color: 'var(--error)' }} aria-hidden>*</span>
          </label>
          <button
            type="button"
            className="text-caption transition-colors duration-200 hover:underline"
            style={{ color: 'var(--accent-400)' }}
            tabIndex={0}
          >
            Forgot password?
          </button>
        </div>

        <div className="relative">
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
            <Lock className="h-4 w-4" style={{ color: 'var(--text-muted)' }} />
          </div>
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            aria-describedby={errors.password ? 'password-error' : undefined}
            aria-invalid={!!errors.password}
            placeholder="••••••••"
            className="w-full rounded-lg py-2.5 pl-10 pr-11 text-body-md outline-none transition-all duration-200"
            style={{
              background: 'var(--bg-elevated)',
              border: `1px solid ${errors.password ? 'var(--error)' : 'var(--border-default)'}`,
              color: 'var(--text-primary)',
              boxShadow: errors.password ? '0 0 0 3px var(--error-bg)' : undefined,
            }}
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute inset-y-0 right-0 flex items-center px-3 transition-colors duration-200"
            style={{ color: 'var(--text-muted)' }}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword
              ? <EyeOff className="h-4 w-4" />
              : <Eye className="h-4 w-4" />
            }
          </button>
        </div>
        {errors.password && (
          <p
            id="password-error"
            role="alert"
            className="mt-1.5 text-caption"
            style={{ color: 'var(--error)' }}
          >
            {errors.password.message}
          </p>
        )}
      </div>

      {/* ── Submit Button ───────────────────────────────────────────── */}
      <button
        type="submit"
        disabled={isLoading}
        className={cn(
          'flex w-full items-center justify-center gap-2 rounded-lg py-2.5',
          'text-body-md font-semibold text-white',
          'transition-all duration-200',
          isLoading
            ? 'cursor-not-allowed opacity-70'
            : 'hover:scale-[1.01] active:scale-[0.99]'
        )}
        style={{
          background: isLoading
            ? 'var(--accent-600)'
            : 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-500) 100%)',
          boxShadow: isLoading ? 'none' : '0 4px 16px rgba(99, 102, 241, 0.4)',
        }}
        aria-busy={isLoading}
      >
        {isLoading ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" />
            Signing in...
          </>
        ) : (
          'Sign in'
        )}
      </button>

      {/* ── Demo credentials hint (dev only) ───────────────────────── */}
      {import.meta.env.DEV && (
        <p className="mt-4 text-center text-caption" style={{ color: 'var(--text-muted)' }}>
          Default admin:{' '}
          <span className="text-code" style={{ color: 'var(--accent-400)' }}>
            admin@commerceinsight.ai
          </span>
          {' / '}
          <span className="text-code" style={{ color: 'var(--accent-400)' }}>
            Admin@123456
          </span>
        </p>
      )}
    </form>
  )
}
