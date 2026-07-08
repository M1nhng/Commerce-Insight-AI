/**
 * lib/utils.ts — Shared utility functions
 * Following shadcn/ui convention for className merging.
 */
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * cn — className utility: merges clsx + tailwind-merge.
 * Resolves Tailwind class conflicts intelligently.
 *
 * @example cn('px-4 py-2', isActive && 'bg-accent-500', className)
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}

/**
 * formatCurrency — Format a number as USD currency.
 */
export function formatCurrency(
  value: number,
  currency = 'USD',
  locale = 'en-US'
): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(value)
}

/**
 * formatNumber — Format a number with thousands separators.
 */
export function formatNumber(value: number, locale = 'en-US'): string {
  return new Intl.NumberFormat(locale).format(value)
}

/**
 * getInitials — Get initials from a full name.
 * @example getInitials("John Doe") → "JD"
 */
export function getInitials(fullName: string): string {
  return fullName
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}
