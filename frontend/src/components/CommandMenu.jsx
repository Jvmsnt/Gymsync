import React, { useEffect } from 'react'
import {
  Bell,
  Download,
  Dumbbell,
  LayoutDashboard,
  Moon,
  Shield,
  Sun,
  UserPlus,
  Users,
} from 'lucide-react'
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
  CommandShortcut,
} from '@/components/ui/command'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { cn } from '@/lib/utils'

export function CommandMenu({
  open,
  onOpenChange,
  members,
  isAdmin,
  dark,
  onToggleDark,
  onNavigate,
  onSelectMember,
  onNewMember,
}) {
  useEffect(() => {
    const onKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        onOpenChange(!open)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, onOpenChange])

  return (
    <CommandDialog
      open={open}
      onOpenChange={onOpenChange}
      showCloseButton={false}
      className="sm:max-w-[580px] border-input bg-card shadow-2xl overflow-hidden"
      title="GymSync Command Palette"
      description="Search members, views, or front desk actions"
    >
      <div className="relative">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-[1.5px] bg-gradient-to-r from-[#D4FF3F]/80 via-[#3DDC97]/40 to-transparent" />
        <CommandInput placeholder="Search athletes, pass #ID, views, or desk actions..." />
      </div>
      <CommandList className="max-h-[340px]">
        <CommandEmpty>No matching members or commands.</CommandEmpty>

        <CommandGroup heading="Instant Member Lookup">
          {members.map((m) => {
            const active = m.status === 'Active'
            const expSoon = active && m.daysRemaining >= 0 && m.daysRemaining <= 7
            return (
              <CommandItem
                key={m.userId}
                value={`${m.fullName} ${m.username} ${m.contactNo} #${m.userId} ${m.fitnessLevel}`}
                onSelect={() => {
                  onOpenChange(false)
                  onSelectMember(m)
                }}
                className="justify-between py-2.5"
              >
                <div className="flex items-center gap-2.5">
                  <Avatar className="h-6 w-6 border border-input">
                    <AvatarFallback className="bg-surface-3 font-mono text-[10px] font-semibold">
                      {m.initials}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <span className="font-medium text-foreground">{m.fullName}</span>
                    <span className="ml-2 font-mono text-xs text-muted-foreground">
                      @{m.username} · #{String(m.userId).padStart(4, '0')}
                    </span>
                  </div>
                </div>
                <span
                  className={cn(
                    'inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 font-mono text-[11px] font-medium tabular-nums',
                    active && !expSoon && 'bg-[#3DDC97]/15 text-[#3DDC97]',
                    expSoon && 'bg-[#FFB547]/15 text-[#FFB547]',
                    !active && 'bg-[#FF5C7A]/15 text-[#FF5C7A]'
                  )}
                >
                  <span
                    className={cn(
                      'h-1.5 w-1.5 rounded-full',
                      active && !expSoon ? 'bg-[#3DDC97]' : expSoon ? 'bg-[#FFB547]' : 'bg-[#FF5C7A]'
                    )}
                  />
                  {m.balanceLabel}
                </span>
              </CommandItem>
            )
          })}
        </CommandGroup>

        <CommandSeparator />

        <CommandGroup heading="Quick Desk Actions">
          <CommandItem
            onSelect={() => {
              onOpenChange(false)
              onNewMember()
            }}
          >
            <UserPlus className="h-4 w-4 text-volt" />
            <span>Register New Member (+1 Month Pass)</span>
            <CommandShortcut>⌘N</CommandShortcut>
          </CommandItem>
          <CommandItem
            onSelect={() => {
              onOpenChange(false)
              onToggleDark()
            }}
          >
            {dark ? <Sun className="h-4 w-4 text-[#FFB547]" /> : <Moon className="h-4 w-4" />}
            <span>Switch to {dark ? 'Daylight Light' : 'Obsidian Volt'} Theme</span>
          </CommandItem>
          {isAdmin && (
            <CommandItem
              onSelect={() => {
                onOpenChange(false)
                window.location.href = '/api/reports/csv'
              }}
            >
              <Download className="h-4 w-4" />
              <span>Export Management Report CSV</span>
              <CommandShortcut>⌘E</CommandShortcut>
            </CommandItem>
          )}
        </CommandGroup>

        <CommandSeparator />

        <CommandGroup heading="Workspace Navigation">
          {[
            { id: 'overview', label: 'Operations Dashboard', icon: LayoutDashboard, kbd: 'G D' },
            { id: 'directory', label: 'Member Directory', icon: Users, kbd: 'G M' },
            { id: 'workouts', label: 'Workouts & Muscle Studio', icon: Dumbbell, kbd: 'G W' },
            { id: 'radar', label: 'Expiry & Renewal Radar', icon: Bell, kbd: 'G R' },
            ...(isAdmin ? [{ id: 'team', label: 'Staff & Operator Accounts', icon: Shield, kbd: 'G S' }] : []),
          ].map((nav) => {
            const Icon = nav.icon
            return (
              <CommandItem
                key={nav.id}
                onSelect={() => {
                  onOpenChange(false)
                  onNavigate(nav.id)
                }}
              >
                <Icon className="h-4 w-4 text-muted-foreground" />
                <span>{nav.label}</span>
                <CommandShortcut>{nav.kbd}</CommandShortcut>
              </CommandItem>
            )
          })}
        </CommandGroup>
      </CommandList>

      <div className="flex items-center justify-between border-t border-border bg-surface-2 px-4 py-2.5 font-mono text-[11px] text-muted-foreground">
        <span>↑↓ Navigate · ↵ Open Pass · ESC Close</span>
        <span className="font-semibold text-volt">GYMSYNC CMDK</span>
      </div>
    </CommandDialog>
  )
}
