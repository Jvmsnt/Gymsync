import React, { useEffect } from 'react'
import { Command } from 'cmdk'
import { BarChart3, Bell, Download, Dumbbell, LayoutDashboard, Moon, Search, Shield, Sun, UserPlus, Users } from 'lucide-react'
import { Badge } from './ui'

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

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center bg-zinc-950/45 backdrop-blur-[2px] pt-[14vh] p-4"
      onClick={() => onOpenChange(false)}
    >
      <div
        className="w-full max-w-lg rounded-2xl border border-zinc-200 bg-white shadow-2xl dark:border-zinc-800 dark:bg-zinc-950 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <Command className="w-full">
          <div className="flex items-center border-b border-zinc-200 px-3.5 dark:border-zinc-800">
            <Search className="h-4 w-4 text-zinc-400 mr-2.5 shrink-0" />
            <Command.Input
              autoFocus
              placeholder="Jump to a member, view, or quick action..."
              className="flex h-11 w-full bg-transparent text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none dark:text-zinc-100"
            />
            <kbd className="rounded border border-zinc-200 px-1.5 py-0.5 font-mono text-[10px] text-zinc-400 dark:border-zinc-800">
              ESC
            </kbd>
          </div>

          <Command.List className="max-h-80 overflow-y-auto p-2 space-y-2">
            <Command.Empty className="py-8 text-center text-xs text-zinc-500">
              No matching members or commands found.
            </Command.Empty>

            <Command.Group heading="Quick Actions" className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400 px-2 py-1">
              <Command.Item
                onSelect={() => {
                  onOpenChange(false)
                  onNewMember()
                }}
                className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium text-zinc-800 aria-selected:bg-zinc-100 dark:text-zinc-200 dark:aria-selected:bg-zinc-900 cursor-pointer"
              >
                <UserPlus className="h-4 w-4 text-emerald-600" />
                <span>Register New Member (+1 Month Pass)</span>
              </Command.Item>
              <Command.Item
                onSelect={() => {
                  onOpenChange(false)
                  onToggleDark()
                }}
                className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium text-zinc-800 aria-selected:bg-zinc-100 dark:text-zinc-200 dark:aria-selected:bg-zinc-900 cursor-pointer"
              >
                {dark ? <Sun className="h-4 w-4 text-amber-500" /> : <Moon className="h-4 w-4 text-zinc-500" />}
                <span>Switch to {dark ? 'Light' : 'Dark'} Mode</span>
              </Command.Item>
              {isAdmin && (
                <Command.Item
                  onSelect={() => {
                    onOpenChange(false)
                    window.location.href = '/api/reports/csv'
                  }}
                  className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium text-zinc-800 aria-selected:bg-zinc-100 dark:text-zinc-200 dark:aria-selected:bg-zinc-900 cursor-pointer"
                >
                  <Download className="h-4 w-4 text-zinc-500" />
                  <span>Export Management Reports CSV</span>
                </Command.Item>
              )}
            </Command.Group>

            <Command.Group heading="Navigation" className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400 px-2 py-1">
              {[
                { id: 'overview', label: 'Overview & Analytics', icon: LayoutDashboard },
                { id: 'directory', label: 'Member Directory', icon: Users },
                { id: 'workouts', label: 'Workouts & Exercise Library', icon: Dumbbell },
                { id: 'radar', label: 'Expiry Alerts Radar', icon: Bell },
                ...(isAdmin ? [{ id: 'team', label: 'Staff & Admin Accounts', icon: Shield }] : []),
              ].map((nav) => {
                const Icon = nav.icon
                return (
                  <Command.Item
                    key={nav.id}
                    onSelect={() => {
                      onOpenChange(false)
                      onNavigate(nav.id)
                    }}
                    className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium text-zinc-800 aria-selected:bg-zinc-100 dark:text-zinc-200 dark:aria-selected:bg-zinc-900 cursor-pointer"
                  >
                    <Icon className="h-4 w-4 text-zinc-400" />
                    <span>{nav.label}</span>
                  </Command.Item>
                )
              })}
            </Command.Group>

            <Command.Group heading="Instant Member Lookup" className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400 px-2 py-1">
              {members.map((m) => (
                <Command.Item
                  key={m.userId}
                  value={`${m.fullName} ${m.username} ${m.contactNo}`}
                  onSelect={() => {
                    onOpenChange(false)
                    onSelectMember(m)
                  }}
                  className="flex items-center justify-between rounded-lg px-2.5 py-2 text-sm text-zinc-800 aria-selected:bg-zinc-100 dark:text-zinc-200 dark:aria-selected:bg-zinc-900 cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-zinc-100 text-[10px] font-bold dark:bg-zinc-800">
                      {m.initials}
                    </span>
                    <div>
                      <span className="font-medium">{m.fullName}</span>
                      <span className="ml-2 text-xs text-zinc-400">@{m.username}</span>
                    </div>
                  </div>
                  <Badge tone={m.status === 'Active' ? 'emerald' : 'rose'}>{m.balanceLabel}</Badge>
                </Command.Item>
              ))}
            </Command.Group>
          </Command.List>
        </Command>
      </div>
    </div>
  )
}
