import React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import * as SelectPrimitive from '@radix-ui/react-select'
import * as TabsPrimitive from '@radix-ui/react-tabs'
import { cva } from 'class-variance-authority'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { Check, ChevronDown, X } from 'lucide-react'

export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-lg font-medium transition-all duration-150 cursor-pointer active:scale-[0.97] disabled:opacity-50 disabled:pointer-events-none select-none',
  {
    variants: {
      variant: {
        primary:
          'bg-emerald-600 text-white hover:bg-emerald-700 shadow-xs dark:bg-emerald-600 dark:hover:bg-emerald-500',
        secondary:
          'bg-white text-zinc-900 border border-zinc-200 hover:bg-zinc-50 shadow-2xs dark:bg-zinc-900 dark:text-zinc-100 dark:border-zinc-800 dark:hover:bg-zinc-800',
        ghost:
          'bg-transparent text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100',
        danger:
          'bg-white text-rose-600 border border-zinc-200 hover:bg-rose-50 hover:border-rose-200 dark:bg-zinc-900 dark:text-rose-400 dark:border-zinc-800 dark:hover:bg-rose-950/50',
      },
      size: {
        sm: 'h-8 px-3 text-xs gap-1.5',
        md: 'h-9 px-3.5 text-sm gap-2',
        lg: 'h-10 px-4 text-sm gap-2',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
)

export function Button({ variant, size, className, children, ...props }) {
  return (
    <button className={cn(buttonVariants({ variant, size }), className)} {...props}>
      {children}
    </button>
  )
}

export function Card({ className, children, ...props }) {
  return (
    <div
      className={cn(
        'rounded-2xl border border-zinc-200/90 bg-white text-zinc-950 shadow-2xs dark:border-zinc-800/90 dark:bg-zinc-950 dark:text-zinc-50',
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}

export function Input({ className, ...props }) {
  return (
    <input
      className={cn(
        'flex h-9 w-full rounded-lg border border-zinc-200 bg-white px-3 py-1 text-sm text-zinc-900 placeholder:text-zinc-400 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 dark:border-zinc-800 dark:bg-zinc-900/60 dark:text-zinc-100 dark:placeholder:text-zinc-500 dark:focus:border-emerald-500',
        className
      )}
      {...props}
    />
  )
}

export function Select({ value, onChange, children, className }) {
  const options = React.Children.toArray(children).map((child) => ({
    value: String(child.props.value ?? child.props.children),
    label: child.props.children,
  }))
  return (
    <SelectPrimitive.Root value={String(value ?? '')} onValueChange={(v) => onChange({ target: { value: v } })}>
      <SelectPrimitive.Trigger
        className={cn(
          'flex h-9 w-full items-center justify-between rounded-lg border border-zinc-200 bg-white px-3 py-1 text-sm text-zinc-900 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 dark:border-zinc-800 dark:bg-zinc-900/60 dark:text-zinc-100 cursor-pointer',
          className
        )}
      >
        <SelectPrimitive.Value />
        <SelectPrimitive.Icon>
          <ChevronDown className="h-4 w-4 text-zinc-400" />
        </SelectPrimitive.Icon>
      </SelectPrimitive.Trigger>
      <SelectPrimitive.Portal>
        <SelectPrimitive.Content
          position="popper"
          sideOffset={4}
          className="z-[100] min-w-[var(--radix-select-trigger-width)] overflow-hidden rounded-xl border border-zinc-200 bg-white p-1 shadow-lg dark:border-zinc-800 dark:bg-zinc-950"
        >
          <SelectPrimitive.Viewport>
            {options.map((opt) => (
              <SelectPrimitive.Item
                key={opt.value}
                value={opt.value}
                className="relative flex cursor-pointer select-none items-center rounded-lg py-1.5 pl-8 pr-3 text-sm text-zinc-800 outline-none data-[highlighted]:bg-zinc-100 dark:text-zinc-200 dark:data-[highlighted]:bg-zinc-900"
              >
                <span className="absolute left-2.5 flex h-3.5 w-3.5 items-center justify-center">
                  <SelectPrimitive.ItemIndicator>
                    <Check className="h-3.5 w-3.5 text-emerald-600" />
                  </SelectPrimitive.ItemIndicator>
                </span>
                <SelectPrimitive.ItemText>{opt.label}</SelectPrimitive.ItemText>
              </SelectPrimitive.Item>
            ))}
          </SelectPrimitive.Viewport>
        </SelectPrimitive.Content>
      </SelectPrimitive.Portal>
    </SelectPrimitive.Root>
  )
}

const badgeVariants = cva(
  'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap tabular-nums',
  {
    variants: {
      tone: {
        neutral: 'bg-zinc-100 text-zinc-700 border-zinc-200 dark:bg-zinc-900 dark:text-zinc-300 dark:border-zinc-800',
        emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-400 dark:border-emerald-900',
        amber: 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/60 dark:text-amber-400 dark:border-amber-900',
        rose: 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/60 dark:text-rose-400 dark:border-rose-900',
        blue: 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-950/60 dark:text-sky-400 dark:border-sky-900',
      },
    },
    defaultVariants: {
      tone: 'neutral',
    },
  }
)

export function Badge({ tone, className, children }) {
  return <span className={cn(badgeVariants({ tone }), className)}>{children}</span>
}

export function Modal({ open, onClose, title, subtitle, maxWidth = 'max-w-lg', children }) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-zinc-950/45 backdrop-blur-[2px]" />
        <DialogPrimitive.Content
          className={cn(
            'fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-zinc-200 bg-white shadow-2xl dark:border-zinc-800 dark:bg-zinc-950 overflow-hidden focus:outline-none',
            maxWidth
          )}
        >
          <div className="flex items-start justify-between border-b border-zinc-200 px-5 py-4 dark:border-zinc-800">
            <div>
              <DialogPrimitive.Title className="text-base font-semibold text-zinc-900 dark:text-zinc-50">
                {title}
              </DialogPrimitive.Title>
              {subtitle && (
                <DialogPrimitive.Description className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
                  {subtitle}
                </DialogPrimitive.Description>
              )}
            </div>
            <DialogPrimitive.Close className="rounded-lg p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-900 dark:hover:text-zinc-200 cursor-pointer">
              <X className="h-4 w-4" />
            </DialogPrimitive.Close>
          </div>
          <div className="p-5 max-h-[80vh] overflow-y-auto">{children}</div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}

export const Tabs = TabsPrimitive.Root
export const TabsList = React.forwardRef(({ className, ...props }, ref) => (
  <TabsPrimitive.List
    ref={ref}
    className={cn(
      'inline-flex h-9 items-center justify-center rounded-lg bg-zinc-100 p-1 text-zinc-500 dark:bg-zinc-900 dark:text-zinc-400 border border-zinc-200/80 dark:border-zinc-800',
      className
    )}
    {...props}
  />
))
export const TabsTrigger = React.forwardRef(({ className, ...props }, ref) => (
  <TabsPrimitive.Trigger
    ref={ref}
    className={cn(
      'inline-flex items-center justify-center whitespace-nowrap rounded-md px-3 py-1 text-xs font-medium transition-all cursor-pointer data-[state=active]:bg-white data-[state=active]:text-zinc-950 data-[state=active]:shadow-2xs dark:data-[state=active]:bg-zinc-800 dark:data-[state=active]:text-zinc-50',
      className
    )}
    {...props}
  />
))
export const TabsContent = TabsPrimitive.Content
