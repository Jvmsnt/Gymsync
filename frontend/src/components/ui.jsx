import React from 'react'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { X } from 'lucide-react'

export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

export function Card({ className, children, ...props }) {
  return (
    <div
      className={cn(
        'rounded-xl border border-zinc-200 bg-white text-zinc-950 shadow-2xs dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-50',
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}

export function Button({ variant = 'primary', size = 'md', className, children, ...props }) {
  const variants = {
    primary:
      'bg-emerald-600 text-white hover:bg-emerald-700 active:bg-emerald-800 border border-transparent shadow-2xs dark:bg-emerald-600 dark:hover:bg-emerald-500',
    secondary:
      'bg-white text-zinc-900 border border-zinc-200 hover:bg-zinc-50 shadow-2xs dark:bg-zinc-900 dark:text-zinc-100 dark:border-zinc-800 dark:hover:bg-zinc-800',
    ghost:
      'bg-transparent text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100',
    danger:
      'bg-white text-rose-600 border border-zinc-200 hover:bg-rose-50 hover:border-rose-200 dark:bg-zinc-900 dark:text-rose-400 dark:border-zinc-800 dark:hover:bg-rose-950/50',
  }
  const sizes = {
    sm: 'h-8 px-3 text-xs gap-1.5',
    md: 'h-9 px-3.5 text-sm gap-2',
    lg: 'h-10 px-4 text-sm gap-2',
  }
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center rounded-lg font-medium transition-colors cursor-pointer disabled:opacity-50 disabled:pointer-events-none',
        variants[variant],
        sizes[size],
        className
      )}
      {...props}
    >
      {children}
    </button>
  )
}

export function Input({ className, ...props }) {
  return (
    <input
      className={cn(
        'flex h-9 w-full rounded-lg border border-zinc-200 bg-white px-3 py-1 text-sm text-zinc-900 placeholder:text-zinc-400 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-100 dark:placeholder:text-zinc-500 dark:focus:border-emerald-500',
        className
      )}
      {...props}
    />
  )
}

export function Select({ className, children, ...props }) {
  return (
    <select
      className={cn(
        'flex h-9 w-full rounded-lg border border-zinc-200 bg-white px-3 py-1 text-sm text-zinc-900 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-100 dark:focus:border-emerald-500',
        className
      )}
      {...props}
    >
      {children}
    </select>
  )
}

export function Badge({ tone = 'neutral', className, children }) {
  const tones = {
    neutral: 'bg-zinc-100 text-zinc-700 border-zinc-200 dark:bg-zinc-900 dark:text-zinc-300 dark:border-zinc-800',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-400 dark:border-emerald-900',
    amber: 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/60 dark:text-amber-400 dark:border-amber-900',
    rose: 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/60 dark:text-rose-400 dark:border-rose-900',
    blue: 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-950/60 dark:text-sky-400 dark:border-sky-900',
  }
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap',
        tones[tone],
        className
      )}
    >
      {children}
    </span>
  )
}

export function Modal({ open, onClose, title, subtitle, maxWidth = 'max-w-lg', children }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/50 backdrop-blur-[2px] p-4">
      <div
        className={cn(
          'w-full rounded-xl border border-zinc-200 bg-white shadow-xl dark:border-zinc-800 dark:bg-zinc-950 overflow-hidden',
          maxWidth
        )}
      >
        <div className="flex items-start justify-between border-b border-zinc-200 px-5 py-4 dark:border-zinc-800">
          <div>
            <h3 className="text-base font-semibold text-zinc-900 dark:text-zinc-50">{title}</h3>
            {subtitle && <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-900 dark:hover:text-zinc-200 cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="p-5 max-h-[80vh] overflow-y-auto">{children}</div>
      </div>
    </div>
  )
}
