import React, { useEffect, useState } from 'react'
import { Drawer } from 'vaul'
import NumberFlow from '@number-flow/react'
import { Calendar, History, MapPin, Pencil, Phone, RefreshCw, ShieldAlert, ShieldCheck, Trash2, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Progress } from '@/components/ui/progress'
import { ScrollArea } from '@/components/ui/scroll-area'
import { BorderBeam } from '@/components/ui/border-beam'
import { MagicCard } from '@/components/ui/magic-card'
import { cn } from '@/lib/utils'

export function MemberDrawer({ member, open, onOpenChange, isAdmin, onRenew, onEdit, onDelete }) {
  const [history, setHistory] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)

  useEffect(() => {
    if (!open || !member?.userId) return
    let active = true
    setHistoryLoading(true)
    fetch(`/api/members/${member.userId}/memberships`)
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error('Failed to load cycle history'))))
      .then((data) => {
        if (active) setHistory(data.memberships || [])
      })
      .catch(() => {
        if (active) setHistory([])
      })
      .finally(() => {
        if (active) setHistoryLoading(false)
      })
    return () => {
      active = false
    }
  }, [open, member?.userId, member?.endDate])

  if (!member) return null
  const isActive = member.status === 'Active'
  const isExpiring = isActive && member.daysRemaining >= 0 && member.daysRemaining <= 7
  const pct = isActive ? Math.max(6, Math.min(100, Math.round((member.daysRemaining / 30) * 100))) : 100

  return (
    <Drawer.Root direction="right" open={open} onOpenChange={onOpenChange}>
      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 z-40 bg-zinc-950/45 backdrop-blur-[2px]" />
        <Drawer.Content
          aria-describedby={undefined}
          className="fixed right-2.5 top-2.5 bottom-2.5 z-50 flex w-full max-w-[440px] flex-col rounded-2xl border bg-background text-foreground shadow-2xl outline-none overflow-hidden"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 border-b">
            <div className="flex items-center gap-3">
              <Avatar className="h-9 w-9 border">
                <AvatarFallback className="text-xs font-semibold">{member.initials}</AvatarFallback>
              </Avatar>
              <div>
                <Drawer.Title className="text-sm font-semibold leading-none">{member.fullName}</Drawer.Title>
                <p className="mt-1 font-mono text-[11px] text-muted-foreground">
                  Pass #{String(member.userId).padStart(4, '0')} · @{member.username}
                </p>
              </div>
            </div>
            <Button variant="ghost" size="icon-sm" onClick={() => onOpenChange(false)} aria-label="Close drawer">
              <X className="h-4 w-4" />
            </Button>
          </div>

          <div className="flex-1 overflow-y-auto p-5 space-y-5">
            {/* THE ONE-STANDOUT COMPONENT: Obsidian Digital Member Pass with MagicCard + BorderBeam */}
            <div className="relative rounded-2xl overflow-hidden shadow-lg bg-zinc-950 text-zinc-50 border border-zinc-800/90">
              <MagicCard
                className="rounded-2xl"
                gradientColor="#18181b"
                gradientFrom={isActive ? '#10b981' : '#f43f5e'}
                gradientTo="#06b6d4"
                gradientOpacity={0.35}
              >
                <div className="relative p-5 space-y-5 bg-zinc-950/95">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="text-[10px] font-mono uppercase tracking-[0.14em] text-zinc-400">
                        GymSync Studio Pass
                      </span>
                      <p className="mt-1 text-lg font-bold tracking-tight text-white">{member.fullName}</p>
                      <p className="text-xs text-zinc-400">{member.fitnessLevel} Tier Athlete</p>
                    </div>
                    <span
                      className={cn(
                        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-medium',
                        isActive && !isExpiring && 'border-emerald-500/30 bg-emerald-500/15 text-emerald-300',
                        isExpiring && 'border-amber-500/30 bg-amber-500/15 text-amber-300',
                        !isActive && 'border-rose-500/30 bg-rose-500/15 text-rose-300'
                      )}
                    >
                      {isActive ? <ShieldCheck className="h-3 w-3" /> : <ShieldAlert className="h-3 w-3" />}
                      {isActive ? (isExpiring ? 'Expiring Soon' : 'Entry Cleared') : 'Expired'}
                    </span>
                  </div>

                  <div className="flex items-end justify-between border-t border-zinc-800/80 pt-4">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">
                        Cycle Balance
                      </p>
                      <div className="mt-0.5 flex items-baseline gap-1.5">
                        <span className="text-3xl font-bold tabular-nums tracking-tight text-white">
                          <NumberFlow value={Math.abs(member.daysRemaining ?? 0)} />
                        </span>
                        <span className="text-xs text-zinc-400">
                          {member.daysRemaining >= 0 ? 'days left' : 'days overdue'}
                        </span>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">Valid Thru</p>
                      <p className="font-mono text-xs text-zinc-200 mt-0.5">{member.endDate}</p>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <Progress
                      value={pct}
                      className={cn(
                        'h-1.5 bg-zinc-800',
                        isActive ? '[&>div]:bg-emerald-500' : '[&>div]:bg-rose-500'
                      )}
                    />
                    <div className="flex items-center justify-between font-mono text-[10px] text-zinc-500">
                      <span>START {member.startDate}</span>
                      <span>CYCLE {pct}%</span>
                    </div>
                  </div>
                </div>
              </MagicCard>
              <BorderBeam
                size={110}
                duration={8}
                colorFrom={isActive ? '#10b981' : '#f43f5e'}
                colorTo="#14b8a6"
                borderWidth={1.5}
              />
            </div>

            {/* Structured Profile Details */}
            <div className="rounded-xl border bg-card">
              <div className="px-4 py-2.5 border-b bg-muted/40">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Athlete Dossier
                </span>
              </div>
              <div className="divide-y text-sm">
                <div className="grid grid-cols-2 gap-4 px-4 py-3">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Level</p>
                    <div className="mt-1">
                      <Badge variant="secondary">{member.fitnessLevel}</Badge>
                    </div>
                  </div>
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Gender</p>
                    <p className="mt-1 font-medium text-xs">{member.gender || 'Unspecified'}</p>
                  </div>
                </div>

                <div className="px-4 py-3 flex items-start gap-2.5">
                  <Phone className="h-3.5 w-3.5 text-muted-foreground mt-0.5 shrink-0" />
                  <div className="min-w-0">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                      Contact &amp; Address
                    </p>
                    <p className="mt-0.5 text-xs font-medium tabular-nums">{member.contactNo}</p>
                    {member.address && (
                      <p className="mt-0.5 text-xs text-muted-foreground flex items-center gap-1">
                        <MapPin className="h-3 w-3 shrink-0" />
                        <span className="truncate">{member.address}</span>
                      </p>
                    )}
                  </div>
                </div>

                <div className="px-4 py-3 flex items-start gap-2.5">
                  <ShieldAlert className="h-3.5 w-3.5 text-muted-foreground mt-0.5 shrink-0" />
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                      Emergency Contact
                    </p>
                    <p className="mt-0.5 text-xs font-medium">
                      {member.emergencyContactName || 'N/A'}{' '}
                      <span className="font-mono text-muted-foreground">({member.emergencyContactNo || 'N/A'})</span>
                    </p>
                  </div>
                </div>

                <div className="px-4 py-3 flex items-start gap-2.5">
                  <Calendar className="h-3.5 w-3.5 text-muted-foreground mt-0.5 shrink-0" />
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                      Next +1 Month Renewal Preview
                    </p>
                    <p className="mt-0.5 font-mono text-xs text-emerald-600 dark:text-emerald-400">
                      {member.nextRenewalStart} → {member.nextRenewalEnd}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Pass Renewal Cycle History */}
            <div className="rounded-xl border bg-card">
              <div className="px-4 py-2.5 border-b bg-muted/40 flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  <History className="h-3.5 w-3.5" />
                  Pass Cycle History
                </span>
                <Badge variant="outline" className="font-mono text-[10px] tabular-nums">
                  {history.length} {history.length === 1 ? 'cycle' : 'cycles'}
                </Badge>
              </div>
              {historyLoading ? (
                <div className="px-4 py-4 text-xs text-muted-foreground">Loading membership cycles...</div>
              ) : history.length === 0 ? (
                <div className="px-4 py-4 text-xs text-muted-foreground">No pass history recorded.</div>
              ) : (
                <ScrollArea className={history.length > 3 ? 'h-44' : 'max-h-44'}>
                  <div className="divide-y">
                    {history.map((cycle) => {
                      const cycleActive = cycle.status === 'Active'
                      return (
                        <div key={cycle.membershipId} className="flex items-center justify-between gap-3 px-4 py-2.5">
                          <div className="min-w-0">
                            <p className="font-mono text-xs font-medium tabular-nums">
                              {cycle.startDate} → {cycle.endDate}
                            </p>
                            <p className="font-mono text-[10px] text-muted-foreground tabular-nums mt-0.5">
                              Cycle #{String(cycle.membershipId).padStart(4, '0')}
                              {cycle.isCurrent ? ' · Latest' : ''}
                            </p>
                          </div>
                          <Badge
                            variant={cycle.isCurrent && cycleActive ? 'secondary' : 'outline'}
                            className={cn(
                              'text-[10px] font-medium shrink-0',
                              cycle.isCurrent &&
                                cycleActive &&
                                'bg-emerald-500/12 text-emerald-700 dark:text-emerald-400 border-emerald-500/25',
                              cycle.isCurrent &&
                                !cycleActive &&
                                'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/25'
                            )}
                          >
                            {cycle.isCurrent ? cycle.status : 'Completed'}
                          </Badge>
                        </div>
                      )
                    })}
                  </div>
                </ScrollArea>
              )}
            </div>
          </div>

          <Separator />
          <div className="p-4 bg-muted/30 space-y-2">
            <div className="grid grid-cols-2 gap-2">
              <Button onClick={() => onRenew(member)} className="w-full cursor-pointer">
                <RefreshCw className="h-3.5 w-3.5" />
                Renew +1 Month
              </Button>
              <Button variant="outline" onClick={() => onEdit(member)} className="w-full cursor-pointer">
                <Pencil className="h-3.5 w-3.5" />
                Edit Profile
              </Button>
            </div>
            {isAdmin && (
              <Button
                variant="ghost"
                className="w-full text-rose-600 hover:bg-rose-50 hover:text-rose-700 dark:text-rose-400 dark:hover:bg-rose-950/40 cursor-pointer"
                onClick={() => onDelete(member)}
              >
                <Trash2 className="h-3.5 w-3.5" />
                Remove Member Record
              </Button>
            )}
          </div>
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  )
}
