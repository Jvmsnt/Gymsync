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
      title="GymSync Command Palette"
      description="Search members, views, or front desk actions"
    >
      <CommandInput placeholder="Search members, views, or commands..." />
      <CommandList>
        <CommandEmpty>No matching members or commands.</CommandEmpty>

        <CommandGroup heading="Quick Actions">
          <CommandItem
            onSelect={() => {
              onOpenChange(false)
              onNewMember()
            }}
          >
            <UserPlus className="h-4 w-4 text-emerald-600" />
            <span>Register New Member (+1 Month Pass)</span>
            <CommandShortcut>NEW</CommandShortcut>
          </CommandItem>
          <CommandItem
            onSelect={() => {
              onOpenChange(false)
              onToggleDark()
            }}
          >
            {dark ? <Sun className="h-4 w-4 text-amber-500" /> : <Moon className="h-4 w-4" />}
            <span>Switch to {dark ? 'Alabaster Light' : 'Obsidian Dark'} Theme</span>
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
              <CommandShortcut>CSV</CommandShortcut>
            </CommandItem>
          )}
        </CommandGroup>

        <CommandSeparator />

        <CommandGroup heading="Workspace Views">
          {[
            { id: 'overview', label: 'Operations Dashboard', icon: LayoutDashboard },
            { id: 'directory', label: 'Member Directory', icon: Users },
            { id: 'workouts', label: 'Workouts & Exercise Library', icon: Dumbbell },
            { id: 'radar', label: 'Expiry & Renewal Radar', icon: Bell },
            ...(isAdmin ? [{ id: 'team', label: 'Staff & Operator Accounts', icon: Shield }] : []),
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
              </CommandItem>
            )
          })}
        </CommandGroup>

        <CommandSeparator />

        <CommandGroup heading={`Member Roster (${members.length})`}>
          {members.map((m) => {
            const active = m.status === 'Active'
            return (
              <CommandItem
                key={m.userId}
                value={`${m.fullName} ${m.username} ${m.contactNo} ${m.fitnessLevel}`}
                onSelect={() => {
                  onOpenChange(false)
                  onSelectMember(m)
                }}
                className="justify-between"
              >
                <div className="flex items-center gap-2.5">
                  <Avatar className="h-6 w-6">
                    <AvatarFallback className="text-[10px] font-semibold">{m.initials}</AvatarFallback>
                  </Avatar>
                  <div>
                    <span className="font-medium">{m.fullName}</span>
                    <span className="ml-2 font-mono text-xs text-muted-foreground">@{m.username}</span>
                  </div>
                </div>
                <span
                  className={cn(
                    'rounded-full px-2 py-0.5 text-[11px] font-medium tabular-nums',
                    active
                      ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400'
                      : 'bg-rose-500/10 text-rose-700 dark:text-rose-400'
                  )}
                >
                  {m.balanceLabel}
                </span>
              </CommandItem>
            )
          })}
        </CommandGroup>
      </CommandList>
    </CommandDialog>
  )
}
