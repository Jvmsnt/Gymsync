import React from 'react'
import { Switch as BaseSwitch } from '@base-ui/react/switch'
import { Menu as BaseMenu } from '@base-ui/react/menu'
import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'

export function StudioSwitch({ checked, onCheckedChange, label, description, id }) {
  return (
    <div className="flex items-center justify-between gap-3">
      {(label || description) && (
        <label htmlFor={id} className="cursor-pointer select-none">
          <div className="text-xs font-medium text-foreground">{label}</div>
          {description && <div className="text-[11px] text-muted-foreground">{description}</div>}
        </label>
      )}
      <BaseSwitch.Root
        id={id}
        checked={checked}
        onCheckedChange={onCheckedChange}
        className={cn(
          'relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border border-transparent p-0.5 transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring',
          checked
            ? 'bg-zinc-900 dark:bg-[#D4FF3F]'
            : 'bg-zinc-300 dark:bg-[#2E323A]'
        )}
      >
        <BaseSwitch.Thumb
          className={cn(
            'pointer-events-none block h-4 w-4 rounded-full shadow-xs transition-transform duration-150',
            checked
              ? 'translate-x-4 bg-white dark:bg-[#0A0B0D]'
              : 'translate-x-0 bg-white dark:bg-[#A0A2AA]'
          )}
        />
      </BaseSwitch.Root>
    </div>
  )
}

export function BaseActionMenu({ trigger, items }) {
  return (
    <BaseMenu.Root>
      <BaseMenu.Trigger className="outline-none w-full text-left">{trigger}</BaseMenu.Trigger>
      <BaseMenu.Portal>
        <BaseMenu.Positioner side="top" align="start" sideOffset={8} className="z-50">
          <BaseMenu.Popup className="min-w-52 rounded-xl border border-input bg-popover p-1.5 text-popover-foreground shadow-2xl outline-none">
            {items.map((item, idx) =>
              item.separator ? (
                <BaseMenu.Separator key={`sep-${idx}`} className="my-1 h-px bg-border" />
              ) : (
                <BaseMenu.Item
                  key={item.label}
                  onClick={item.onClick}
                  className={cn(
                    'flex cursor-pointer select-none items-center justify-between rounded-lg px-2.5 py-2 text-xs font-medium outline-none transition-colors data-[highlighted]:bg-accent data-[highlighted]:text-accent-foreground',
                    item.danger && 'text-[#FF5C7A] data-[highlighted]:bg-[#FF5C7A]/12'
                  )}
                >
                  <span className="flex items-center gap-2">
                    {item.icon}
                    {item.label}
                  </span>
                  {item.checked && <Check className="h-3.5 w-3.5 text-[#D4FF3F]" />}
                </BaseMenu.Item>
              )
            )}
          </BaseMenu.Popup>
        </BaseMenu.Positioner>
      </BaseMenu.Portal>
    </BaseMenu.Root>
  )
}
