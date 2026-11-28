import React, { useEffect, useState } from 'react'
import { Drawer } from 'vaul'
import NumberFlow from '@number-flow/react'
import {
  Calendar,
  History,
  MapPin,
  Pencil,
  Phone,
  QrCode,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Trash2,
  X,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
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
        <Drawer.Overlay className="fixed inset-0 z-40 bg-black/60 backdrop-blur-[3px]" />
        <Drawer.Content
          aria-describedby={undefined}
          className="fixed right-3 top-3 bottom-3 z-50 flex w-full max-w-[448px] flex-col rounded-2xl border border-input bg-card text-foreground shadow-2xl outline-none overflow-hidden"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-border bg-card">
            <div className="flex items-center gap-3">
              <Avatar className="h-9 w-9 border border-input">
                <AvatarFallback className="bg-surface-3 font-mono text-xs font-semibold text-foreground">
                  {member.initials}
                </AvatarFallback>
              </Avatar>
              <div>
                <Drawer.Title className="text-sm font-semibold leading-none">{member.fullName}</Drawer.Title>
                <p className="mt-1 font-mono text-[11px] text-muted-foreground">
                  PASS #{String(member.userId).padStart(4, '0')} · @{member.username}
                </p>
              </div>
            </div>
            <Button variant="ghost" size="icon-sm" onClick={() => onOpenChange(false)} aria-label="Close drawer">
              <X className="h-4 w-4" />
            </Button>
          </div>

          <div className="flex-1 overflow-y-auto px-6 py-5 space-y-5">
            {/* THE ONE-STANDOUT HERO ARTIFACT: Obsidian + Electric Volt Digital Member Pass */}
            <div className="relative rounded-2xl overflow-hidden shadow-2xl border border-[#2E323A] obsidian-pass-surface text-[#F4F4F1]">
              <MagicCard
                className="rounded-2xl"
                gradientColor="#181A1F"
                gradientFrom={isActive ? '#D4FF3F' : '#FF5C7A'}
                gradientTo="#3DDC97"
                gradientOpacity={0.28}
              >
                <div className="relative p-5 space-y-4">
                  <div
                    className={cn(
                      'pointer-events-none absolute -top-16 -right-12 h-44 w-44 rounded-full blur-3xl opacity-20',
                      isActive ? 'bg-[#D4FF3F]' : 'bg-[#FF5C7A]'
                    )}
                  />

                  <div className="relative space-y-1.5">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <span className="text-[10px] font-mono uppercase tracking-[0.14em] text-[#D4FF3F]">
                          GymSync Studio Access · 1-Mo
                        </span>
                        <p className="font-display mt-1 text-xl font-bold tracking-tight text-[#F4F4F1]">
                          {member.fullName}
                        </p>
                      </div>
                      <span
                        className={cn(
                          'shrink-0 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-mono text-[11px] font-medium',
                          isActive && !isExpiring && 'bg-[#3DDC97]/15 text-[#3DDC97]',
                          isExpiring && 'bg-[#FFB547]/15 text-[#FFB547]',
                          !isActive && 'bg-[#FF5C7A]/15 text-[#FF5C7A]'
                        )}
                      >
                        {isActive ? <ShieldCheck className="h-3 w-3" /> : <ShieldAlert className="h-3 w-3" />}
                        {isActive ? (isExpiring ? 'Expiring' : 'Entry Cleared') : 'Expired'}
                      </span>
                    </div>
                    <p className="font-mono text-[11px] text-[#A0A2AA]">
                      PASS #{String(member.userId).padStart(4, '0')} · @{member.username} ·{' '}
                      {(member.fitnessLevel || 'Member').toUpperCase()}
                    </p>
                  </div>

                  <div className="h-px bg-[#2E323A]/70" />

                  <div className="relative flex items-end justify-between gap-4">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-[0.1em] text-[#666973]">
                        Cycle Balance
                      </p>
                      <div className="mt-0.5 flex items-baseline gap-2">
                        <span
                          className={cn(
                            'font-display text-4xl font-bold tabular-nums tracking-tight leading-none',
                            isActive ? 'text-[#D4FF3F]' : 'text-[#FF5C7A]'
                          )}
                        >
                          <NumberFlow value={Math.abs(member.daysRemaining ?? 0)} />
                        </span>
                        <span className="text-xs font-medium text-[#A0A2AA]">
                          {member.daysRemaining >= 0 ? 'days left' : 'days overdue'}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className="text-right">
                        <p className="text-[10px] font-semibold uppercase tracking-[0.1em] text-[#666973]">
                          Valid Thru
                        </p>
                        <p className="font-mono text-xs font-medium text-[#F4F4F1] mt-0.5 tabular-nums">
                          {member.endDate}
                        </p>
                      </div>
                      <div className="flex h-11 w-11 items-center justify-center rounded-xl border border-[#2E323A] bg-[#202329]/90">
                        <QrCode className={cn('h-6 w-6', isActive ? 'text-[#D4FF3F]' : 'text-[#FF5C7A]')} />
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-[#202329]">
                      <div
                        className="h-full rounded-full transition-all duration-500"
                        style={{
                          width: `${pct}%`,
                          background: isActive
                            ? 'linear-gradient(90deg, #3DDC97 0%, #D4FF3F 100%)'
                            : '#FF5C7A',
                        }}
                      />
                    </div>
                    <div className="flex items-center justify-between font-mono text-[10px] text-[#666973] tabular-nums">
                      <span>START {member.startDate}</span>
                      <span className="text-[#A0A2AA]">{pct}% REMAINING</span>
                    </div>
                  </div>
                </div>
              </MagicCard>
              <BorderBeam
                size={120}
                duration={7}
                colorFrom={isActive ? '#D4FF3F' : '#FF5C7A'}
                colorTo="#3DDC97"
                borderWidth={1.5}
              />
            </div>

            {/* Structured Athlete Dossier */}
            <div className="rounded-xl border border-border bg-surface-2 overflow-hidden">
              <div className="px-4 py-2.5 border-b border-border bg-surface-3/60">
                <span className="text-[10px] font-semibold uppercase tracking-[0.1em] text-muted-foreground">
                  Athlete Dossier
                </span>
              </div>
              <div className="divide-y divide-border text-sm">
                <div className="px-4 py-3">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                    Fitness Tier &amp; Profile
                  </p>
                  <p className="mt-1 text-xs font-medium text-foreground">
                    {member.fitnessLevel} · {member.gender || 'Unspecified'}
                    {member.birthdate ? ` · Born ${member.birthdate}` : ''}
                  </p>
                </div>

                <div className="px-4 py-3 flex items-start gap-2.5">
                  <Phone className="h-3.5 w-3.5 text-muted-foreground mt-0.5 shrink-0" />
                  <div className="min-w-0">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                      Contact &amp; Address
                    </p>
                    <p className="mt-0.5 text-xs font-medium font-mono tabular-nums">{member.contactNo}</p>
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
                    <p className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
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
                    <p className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                      Next +1 Month Renewal Preview
                    </p>
                    <p className="mt-0.5 font-mono text-xs font-medium text-volt tabular-nums">
                      {member.nextRenewalStart} → {member.nextRenewalEnd}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Pass Renewal Cycle History */}
            <div className="rounded-xl border border-border bg-surface-2 overflow-hidden">
              <div className="px-4 py-2.5 border-b border-border bg-surface-3/60 flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.1em] text-muted-foreground">
                  <History className="h-3.5 w-3.5" />
                  Pass Cycle History
                </span>
                <Badge variant="outline" className="font-mono text-[10px] tabular-nums border-input">
                  {history.length} {history.length === 1 ? 'cycle' : 'cycles'}
                </Badge>
              </div>
              {historyLoading ? (
                <div className="px-4 py-4 text-xs text-muted-foreground">Loading membership cycles...</div>
              ) : history.length === 0 ? (
                <div className="px-4 py-4 text-xs text-muted-foreground">No pass history recorded.</div>
              ) : (
                <ScrollArea className={history.length > 3 ? 'h-40' : 'max-h-40'}>
                  <div className="divide-y divide-border">
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
                          <span
                            className={cn(
                              'rounded-full px-2 py-0.5 font-mono text-[10px] font-medium shrink-0',
                              cycle.isCurrent && cycleActive && 'bg-[#3DDC97]/15 text-[#3DDC97]',
                              cycle.isCurrent && !cycleActive && 'bg-[#FF5C7A]/15 text-[#FF5C7A]',
                              !cycle.isCurrent && 'bg-surface-3 text-muted-foreground'
                            )}
                          >
                            {cycle.isCurrent ? cycle.status : 'Completed'}
                          </span>
                        </div>
                      )
                    })}
                  </div>
                </ScrollArea>
              )}
            </div>
          </div>

          <Separator />
          <div className="px-6 py-4 bg-surface-2 space-y-2.5">
            <div className="grid grid-cols-2 gap-2.5">
              <Button onClick={() => onRenew(member)} className="w-full cursor-pointer active:scale-[0.97]">
                <RefreshCw className="h-3.5 w-3.5" />
                Renew +1 Month
              </Button>
              <Button variant="outline" onClick={() => onEdit(member)} className="w-full cursor-pointer active:scale-[0.97]">
                <Pencil className="h-3.5 w-3.5" />
                Edit Profile
              </Button>
            </div>
            {isAdmin && (
              <Button
                variant="ghost"
                size="sm"
                className="w-full bg-[#FF5C7A]/10 text-[#FF5C7A] hover:bg-[#FF5C7A]/20 hover:text-[#FF5C7A] cursor-pointer active:scale-[0.97]"
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
