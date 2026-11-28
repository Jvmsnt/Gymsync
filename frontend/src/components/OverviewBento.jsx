import React, { useMemo, useState } from 'react'
import NumberFlow from '@number-flow/react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { ArrowUpRight, RefreshCw, ScanLine } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { BlurFade } from '@/components/ui/blur-fade'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'

export function OverviewBento({
  members,
  report,
  onSelectMember,
  onRenewMember,
  onNavigate,
}) {
  const [scanQuery, setScanQuery] = useState('')

  const activeCount = members.filter((m) => m.status === 'Active').length
  const expiredCount = members.filter((m) => m.status !== 'Active').length
  const expiringList = members.filter((m) => m.status === 'Active' && m.daysRemaining >= 0 && m.daysRemaining <= 7)
  const healthyCount = Math.max(0, activeCount - expiringList.length)

  const urgentQueue = useMemo(() => {
    const q = scanQuery.trim().toLowerCase()
    if (q) {
      return members
        .filter(
          (m) =>
            m.fullName.toLowerCase().includes(q) ||
            m.username.toLowerCase().includes(q) ||
            String(m.userId).includes(q.replace('#', ''))
        )
        .slice(0, 6)
    }
    return members
      .filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7))
      .slice(0, 5)
  }, [members, scanQuery])

  const totalUrgent = members.filter(
    (m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7)
  ).length

  const chartData = [...(report?.monthlyRenewals || [])]
    .reverse()
    .map((item) => ({ month: item.month, renewals: item.count }))

  const activePct = members.length ? Math.round((activeCount / members.length) * 100) : 0
  const todayLabel = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
  })

  return (
    <div className="space-y-6">
      {/* Studio Hero Header */}
      <BlurFade delay={0.02}>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-[11px] font-mono font-semibold uppercase tracking-[0.12em] text-volt">
              Front Desk Command Center
            </p>
            <h1 className="font-display mt-1 text-2xl sm:text-3xl font-bold tracking-tight">
              Operations Overview
            </h1>
          </div>
          <p className="font-mono text-xs text-muted-foreground">
            {todayLabel} · 1-Month Cycle Active
          </p>
        </div>
      </BlurFade>

      {/* 4-Card KPI Strip (Matches Figma Metric Card Component) */}
      <BlurFade delay={0.05}>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricTile
            label="Total Roster"
            value={members.length}
            delta="Live DB"
            footnote="Registered studio athletes"
            dotColor="bg-foreground"
            deltaColor="text-muted-foreground"
          />
          <MetricTile
            label="Active Passes"
            value={activeCount}
            delta={`${activePct}% health`}
            footnote="Entry cleared at desk"
            dotColor="bg-[#3DDC97]"
            deltaColor="text-[#3DDC97]"
          />
          <MetricTile
            label="Expiring ≤ 7d"
            value={expiringList.length}
            delta="Due soon"
            footnote="Needs renewal callout"
            dotColor="bg-[#FFB547]"
            deltaColor="text-[#FFB547]"
          />
          <MetricTile
            label="Expired Passes"
            value={expiredCount}
            delta="Blocked"
            footnote="Requires +1 mo renewal"
            dotColor="bg-[#FF5C7A]"
            deltaColor="text-[#FF5C7A]"
          />
        </div>
      </BlurFade>

      {/* Asymmetric 7/5 Bento Grid */}
      <BlurFade delay={0.09}>
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-stretch">
          {/* Left 7 cols: Volt Area Chart + Roster Health Composition */}
          <Card className="xl:col-span-7 py-5 gap-4 border-border bg-card shadow-none flex flex-col justify-between">
            <div className="px-5 flex items-start justify-between gap-3">
              <div>
                <CardTitle className="text-base">Monthly Pass Volume &amp; Renewals</CardTitle>
                <CardDescription className="text-xs mt-1">
                  New activations &amp; 1-month renewals across cycles
                </CardDescription>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onNavigate('directory')}
                className="shrink-0 text-muted-foreground hover:text-foreground cursor-pointer"
              >
                View Roster
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Button>
            </div>

            <CardContent className="px-5 space-y-5 flex-1 flex flex-col justify-between">
              <div className="h-56 w-full">
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={chartData} margin={{ top: 10, right: 26, left: -20, bottom: 0 }}>
                      <defs>
                        <linearGradient id="voltAreaFill" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#D4FF3F" stopOpacity={0.34} />
                          <stop offset="68%" stopColor="#3DDC97" stopOpacity={0.08} />
                          <stop offset="100%" stopColor="#3DDC97" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="rgba(160,162,170,0.16)" />
                      <XAxis
                        dataKey="month"
                        padding={{ left: 10, right: 10 }}
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 11, fill: '#666973', fontFamily: 'Geist Mono, monospace' }}
                      />
                      <YAxis
                        allowDecimals={false}
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 11, fill: '#666973', fontFamily: 'Geist Mono, monospace' }}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#181A1F',
                          borderColor: 'rgba(212,255,63,0.45)',
                          borderRadius: '10px',
                          fontSize: '12px',
                          fontFamily: 'Geist Mono, monospace',
                          color: '#F4F4F1',
                        }}
                        itemStyle={{ color: '#D4FF3F' }}
                      />
                      <Area
                        type="monotone"
                        dataKey="renewals"
                        stroke="#D4FF3F"
                        strokeWidth={2.5}
                        fill="url(#voltAreaFill)"
                        activeDot={{ r: 5, fill: '#D4FF3F', stroke: '#09090B', strokeWidth: 2 }}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-xs text-muted-foreground">
                    No monthly renewal history recorded yet.
                  </div>
                )}
              </div>

              {/* Roster Health Composition */}
              <div className="rounded-xl border border-border bg-surface-2 p-4 space-y-2.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-medium text-foreground">Roster Health Composition</span>
                  <span className="font-mono font-medium text-[#3DDC97] tabular-nums">
                    {activePct}% ACTIVE PASSES
                  </span>
                </div>
                <div className="flex h-2 w-full overflow-hidden rounded-full bg-surface-3 gap-1">
                  {members.length > 0 && (
                    <>
                      <div
                        className="rounded-full bg-[#3DDC97] transition-all duration-500"
                        style={{ width: `${(healthyCount / members.length) * 100}%` }}
                      />
                      <div
                        className="rounded-full bg-[#FFB547] transition-all duration-500"
                        style={{ width: `${(expiringList.length / members.length) * 100}%` }}
                      />
                      <div
                        className="rounded-full bg-[#FF5C7A] transition-all duration-500"
                        style={{ width: `${(expiredCount / members.length) * 100}%` }}
                      />
                    </>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-5 font-mono text-[11px] text-muted-foreground">
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-[#3DDC97]" />
                    Healthy ({healthyCount})
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-[#FFB547]" />
                    Expiring ≤7d ({expiringList.length})
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-[#FF5C7A]" />
                    Expired ({expiredCount})
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Right 5 cols: Front-Desk Triage + Instant Pass Scanner */}
          <Card className="xl:col-span-5 py-5 gap-4 border-border bg-card shadow-none flex flex-col">
            <div className="px-5 flex items-start justify-between gap-3">
              <div>
                <CardTitle className="text-base">Front-Desk Triage</CardTitle>
                <CardDescription className="text-xs mt-1">
                  Urgent renewals &amp; instant pass check-in
                </CardDescription>
              </div>
              <button
                onClick={() => onNavigate('radar')}
                className={cn(
                  'shrink-0 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-mono text-[11px] font-medium cursor-pointer transition-opacity hover:opacity-85',
                  totalUrgent > 0 ? 'bg-[#FF5C7A]/15 text-[#FF5C7A]' : 'bg-[#3DDC97]/15 text-[#3DDC97]'
                )}
              >
                <span className={cn('h-1.5 w-1.5 rounded-full', totalUrgent > 0 ? 'bg-[#FF5C7A]' : 'bg-[#3DDC97]')} />
                {totalUrgent} Urgent
              </button>
            </div>

            <CardContent className="px-5 space-y-3 flex-1 flex flex-col">
              {/* Instant Pass Scanner Bar */}
              <form
                onSubmit={(e) => {
                  e.preventDefault()
                  if (urgentQueue[0]) onSelectMember(urgentQueue[0])
                }}
                className="flex items-center gap-2 rounded-lg border border-input bg-surface-2 px-3 h-10 focus-within:border-volt transition-colors"
              >
                <ScanLine className="h-4 w-4 text-volt shrink-0" />
                <input
                  value={scanQuery}
                  onChange={(e) => setScanQuery(e.target.value)}
                  placeholder="Scan Pass #ID or search athlete..."
                  className="flex-1 bg-transparent text-xs text-foreground placeholder:text-muted-foreground focus:outline-none"
                />
                <span className="font-mono text-[10px] text-muted-foreground">ENTER</span>
              </form>

              {/* Queue Rows */}
              <div className="space-y-2 flex-1">
                {urgentQueue.map((m) => {
                  const isExpired = m.status !== 'Active'
                  const isExpiring = !isExpired && m.daysRemaining >= 0 && m.daysRemaining <= 7
                  return (
                    <div
                      key={m.userId}
                      onClick={() => onSelectMember(m)}
                      className={cn(
                        'flex items-center justify-between gap-3 rounded-xl border px-3 py-2.5 bg-surface-2 hover:bg-surface-3/80 transition-colors cursor-pointer',
                        isExpired ? 'border-[#FF5C7A]/35' : 'border-border'
                      )}
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <Avatar className="h-8 w-8 border border-input shrink-0">
                          <AvatarFallback className="bg-surface-3 font-mono text-[10px] font-semibold">
                            {m.initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <div className="flex items-center gap-1.5">
                            <span
                              className={cn(
                                'h-1.5 w-1.5 rounded-full shrink-0',
                                isExpired ? 'bg-[#FF5C7A]' : isExpiring ? 'bg-[#FFB547]' : 'bg-[#3DDC97]'
                              )}
                            />
                            <p className="truncate text-xs font-semibold text-foreground">{m.fullName}</p>
                          </div>
                          <p className="mt-0.5 font-mono text-[11px] text-muted-foreground tabular-nums truncate">
                            {m.balanceLabel} · {isExpired ? 'Blocked' : `Ends ${m.endDate}`}
                          </p>
                        </div>
                      </div>
                      <Button
                        size="xs"
                        variant="outline"
                        className="shrink-0 border-input bg-surface-3/50 hover:bg-surface-3 cursor-pointer active:scale-[0.97]"
                        onClick={(e) => {
                          e.stopPropagation()
                          onRenewMember(m)
                        }}
                      >
                        <RefreshCw className="h-3 w-3" />
                        +1 Mo
                      </Button>
                    </div>
                  )
                })}

                {urgentQueue.length === 0 && (
                  <div className="py-14 text-center text-xs text-muted-foreground space-y-2.5">
                    <Progress value={100} className="w-24 mx-auto h-1.5 [&>div]:bg-[#3DDC97]" />
                    <p>{scanQuery ? 'No athletes match that pass query.' : 'All studio passes are active and healthy.'}</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </BlurFade>
    </div>
  )
}

function MetricTile({ label, value, delta, footnote, dotColor, deltaColor }) {
  return (
    <Card className="py-4 px-5 gap-2.5 border-border bg-card shadow-none">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-medium uppercase tracking-[0.08em] text-muted-foreground">
          {label}
        </span>
        <span className={cn('h-2 w-2 rounded-full', dotColor)} />
      </div>
      <div className="flex items-baseline gap-2.5">
        <span className="font-display text-4xl font-bold tracking-tight tabular-nums leading-none">
          <NumberFlow value={value} />
        </span>
        {delta && <span className={cn('font-mono text-xs font-medium', deltaColor)}>{delta}</span>}
      </div>
      <p className="text-xs text-muted-foreground">{footnote}</p>
    </Card>
  )
}
