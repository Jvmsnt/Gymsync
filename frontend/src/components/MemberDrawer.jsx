import React from 'react'
import { Drawer } from 'vaul'
import NumberFlow from '@number-flow/react'
import { Pencil, RefreshCw, ShieldCheck, Trash2, X } from 'lucide-react'
import { Badge, Button, cn } from './ui'

export function MemberDrawer({ member, open, onOpenChange, isAdmin, onRenew, onEdit, onDelete }) {
  if (!member) return null
  const isActive = member.status === 'Active'
  const pct = isActive ? Math.max(8, Math.min(100, Math.round((member.daysRemaining / 30) * 100))) : 100

  return (
    <Drawer.Root direction="right" open={open} onOpenChange={onOpenChange}>
      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 z-40 bg-zinc-950/40 backdrop-blur-[2px]" />
        <Drawer.Content
          className="fixed right-2 top-2 bottom-2 z-50 flex w-full max-w-[420px] flex-col rounded-2xl border border-zinc-200 bg-white shadow-2xl outline-none dark:border-zinc-800 dark:bg-zinc-950 overflow-hidden"
        >
          {/* Drawer Header */}
          <div className="flex items-center justify-between border-b border-zinc-200 px-5 py-4 dark:border-zinc-800">
            <div>
              <Drawer.Title className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">
                Member Pass &amp; Profile
              </Drawer.Title>
              <Drawer.Description className="text-xs text-zinc-500">
                Member #{member.userId} • @{member.username}
              </Drawer.Description>
            </div>
            <button
              onClick={() => onOpenChange(false)}
              className="rounded-lg p-1.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-900 dark:hover:text-zinc-200 cursor-pointer"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-5 space-y-5">
            {/* THE ONE-STANDOUT COMPONENT: Tactile Digital Member Pass */}
            <div className="rounded-2xl p-[1px] standout-pass-border shadow-md">
              <div className="rounded-[15px] bg-zinc-900 p-5 text-white space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 border border-emerald-500/40 text-sm font-bold text-emerald-400">
                      {member.initials}
                    </div>
                    <div>
                      <p className="text-base font-semibold tracking-tight">{member.fullName}</p>
                      <p className="font-mono text-xs text-zinc-400">ID #{String(member.userId).padStart(4, '0')}</p>
                    </div>
                  </div>
                  <Badge
                    tone={isActive ? 'emerald' : 'rose'}
                    className={
                      isActive
                        ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                        : 'bg-rose-500/15 text-rose-300 border-rose-500/30'
                    }
                  >
                    <ShieldCheck className="h-3 w-3" />
                    {isActive ? 'Entry Cleared' : 'Expired'}
                  </Badge>
                </div>

                <div className="flex items-end justify-between border-t border-zinc-800 pt-4">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">
                      Cycle Balance
                    </p>
                    <div className="mt-0.5 flex items-baseline gap-1.5">
                      <span className="text-3xl font-bold tabular-nums tracking-tight">
                        <NumberFlow value={Math.abs(member.daysRemaining ?? 0)} />
                      </span>
                      <span className="text-xs text-zinc-400">
                        {member.daysRemaining >= 0 ? 'days left' : 'days overdue'}
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">Expires</p>
                    <p className="font-mono text-xs text-zinc-200 mt-0.5">{member.endDate}</p>
                  </div>
                </div>

                <div className="h-1.5 w-full rounded-full bg-zinc-800 overflow-hidden">
                  <div
                    className={cn('h-full rounded-full transition-all duration-300', isActive ? 'bg-emerald-500' : 'bg-rose-500')}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Structured Profile Details */}
            <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
              <div className="grid grid-cols-2 gap-4 p-3.5">
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Fitness Level</p>
                  <p className="mt-0.5 font-medium">{member.fitnessLevel}</p>
                </div>
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Gender</p>
                  <p className="mt-0.5 font-medium">{member.gender || 'Unspecified'}</p>
                </div>
              </div>
              <div className="p-3.5">
                <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Contact &amp; Address</p>
                <p className="mt-0.5 font-medium">
                  {member.contactNo}
                  {member.address ? ` • ${member.address}` : ''}
                </p>
              </div>
              <div className="p-3.5">
                <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Emergency Contact</p>
                <p className="mt-0.5 font-medium">
                  {member.emergencyContactName || 'N/A'} ({member.emergencyContactNo || 'N/A'})
                </p>
              </div>
              <div className="p-3.5">
                <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Current 1-Month Period</p>
                <p className="mt-0.5 font-mono text-xs">{member.startDate} → {member.endDate}</p>
              </div>
              <div className="p-3.5">
                <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Next Renewal Window</p>
                <p className="mt-0.5 font-mono text-xs text-emerald-600 dark:text-emerald-400">
                  {member.nextRenewalStart} → {member.nextRenewalEnd}
                </p>
              </div>
            </div>
          </div>

          {/* Drawer Footer Actions */}
          <div className="border-t border-zinc-200 p-4 dark:border-zinc-800 space-y-2 bg-zinc-50/70 dark:bg-zinc-900/40">
            <div className="grid grid-cols-2 gap-2">
              <Button onClick={() => onRenew(member)}>
                <RefreshCw className="h-3.5 w-3.5" />
                Renew +1 Month
              </Button>
              <Button variant="secondary" onClick={() => onEdit(member)}>
                <Pencil className="h-3.5 w-3.5" />
                Edit Profile
              </Button>
            </div>
            {isAdmin && (
              <Button variant="danger" className="w-full" onClick={() => onDelete(member)}>
                <Trash2 className="h-3.5 w-3.5" />
                Delete Member
              </Button>
            )}
          </div>
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  )
}
