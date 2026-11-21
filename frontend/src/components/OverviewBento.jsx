import React from 'react'
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
import { ArrowUpRight, Clock, Download, RefreshCw, ShieldAlert, UserPlus, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { BlurFade } from '@/components/ui/blur-fade'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'

export function OverviewBento({
  members,
  report,
  onSelectMember,
  onRenewMember,
  onNewMember,
  onNavigate,
}) {
  const activeCount = members.filter((m) => m.status === 'Active').length
  const expiredCount = members.filter((m) => m.status !== 'Active').length
  const expiringList = members.filter((m) => m.status === 'Active' && m.daysRemaining >= 0 && m.daysRemaining <= 7)
  const healthyCount = Math.max(0, activeCount - expiringList.length)

  const attentionQueue = members
    .filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7))
    .slice(0, 6)

  const chartData = [...(report?.monthlyRenewals || [])]
    .reverse()
    .map((item) => ({ month: item.month, renewals: item.count }))

  const activePct = members.length ? Math.round((activeCount / members.length) * 100) : 0

  return (
    <div className="space-y-6">
      {/* Studio Page Header */}
      <BlurFade delay={0.02}>
        <div className="flex flex-wrap items-end justify-between gap-4 border-b pb-5">
          <div>
            <div className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
              Real-Time Front Desk Operations
            </div>
            <h1 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight">Operations Dashboard</h1>
          </div>
          <div className="flex items-center gap-2">
            <a href="/api/reports/csv" download>
              <Button variant="outline" size="sm" className="cursor-pointer">
                <Download className="h-3.5 w-3.5" />
                Export CSV
              </Button>
            </a>
            <Button size="sm" onClick={onNewMember} className="cursor-pointer">
              <UserPlus className="h-3.5 w-3.5" />
              Register Member
            </Button>
          </div>
        </div>
      </BlurFade>

      {/* Clean Studio Metric Strip — NumberFlow animated digits, zero gradient bloat */}
      <BlurFade delay={0.06}>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricTile
            label="Total Roster"
            value={members.length}
            footnote="Registered athletes"
            dotClass="bg-zinc-900 dark:bg-zinc-100"
            icon={Users}
          />
          <MetricTile
            label="Active Passes"
            value={activeCount}
            suffix={`(${activePct}%)`}
            footnote="Entry cleared today"
            dotClass="bg-emerald-500"
            icon={ShieldAlert}
          />
          <MetricTile
            label="Expiring ≤ 7d"
            value={expiringList.length}
            footnote="Renewal reminders"
            dotClass="bg-amber-500"
            icon={Clock}
          />
          <MetricTile
            label="Expired Passes"
            value={expiredCount}
            footnote="Blocked at desk"
            dotClass="bg-rose-500"
            icon={RefreshCw}
          />
        </div>
      </BlurFade>

      {/* Asymmetric 7/5 Operations Split */}
      <BlurFade delay={0.1}>
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-stretch">
          {/* Chart + Roster Composition */}
          <Card className="xl:col-span-7 py-5 gap-4">
            <div className="px-5 flex items-center justify-between gap-3">
              <div>
                <CardTitle className="text-base">Monthly Pass Volume</CardTitle>
                <CardDescription className="text-xs mt-0.5">
                  1-month memberships issued and renewed across cycles
                </CardDescription>
              </div>
              <Button variant="ghost" size="sm" onClick={() => onNavigate('directory')} className="shrink-0 cursor-pointer">
                View Roster
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Button>
            </div>

            <CardContent className="px-5 space-y-4">
              <div className="h-56 w-full">
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={chartData} margin={{ top: 8, right: 8, left: -24, bottom: 0 }}>
                      <defs>
                        <linearGradient id="studioInkFill" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#10b981" stopOpacity={0.22} />
                          <stop offset="100%" stopColor="#10b981" stopOpacity={0.0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(161,161,170,0.2)" />
                      <XAxis
                        dataKey="month"
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 11, fill: '#71717a' }}
                      />
                      <YAxis
                        allowDecimals={false}
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 11, fill: '#71717a' }}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#18181b',
                          borderColor: '#27272a',
                          borderRadius: '10px',
                          fontSize: '12px',
                          color: '#fafafa',
                        }}
                      />
                      <Area
                        type="monotone"
                        dataKey="renewals"
                        stroke="#10b981"
                        strokeWidth={2}
                        fill="url(#studioInkFill)"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-xs text-muted-foreground">
                    No monthly renewal history recorded yet.
                  </div>
                )}
              </div>

              {/* Clean Segmented Roster Health Bar */}
              <div className="rounded-xl border bg-muted/35 p-3.5 space-y-2.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-medium">Roster Health Breakdown</span>
                  <span className="font-mono text-muted-foreground tabular-nums">{activePct}% healthy</span>
                </div>
                <div className="flex h-2 w-full overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-800 gap-0.5">
                  {members.length > 0 && (
                    <>
                      <div
                        className="bg-emerald-500 transition-all duration-500"
                        style={{ width: `${(healthyCount / members.length) * 100}%` }}
                      />
                      <div
                        className="bg-amber-500 transition-all duration-500"
                        style={{ width: `${(expiringList.length / members.length) * 100}%` }}
                      />
                      <div
                        className="bg-rose-500 transition-all duration-500"
                        style={{ width: `${(expiredCount / members.length) * 100}%` }}
                      />
                    </>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-4 text-[11px] text-muted-foreground">
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-emerald-500" />
                    Healthy ({healthyCount})
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-amber-500" />
                    Due ≤7d ({expiringList.length})
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-rose-500" />
                    Expired ({expiredCount})
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Actionable Desk Triage Queue */}
          <Card className="xl:col-span-5 py-5 gap-3 flex flex-col">
            <div className="px-5 flex items-center justify-between gap-3 pb-1">
              <div>
                <CardTitle className="text-base">Renewal Queue</CardTitle>
                <CardDescription className="text-xs mt-0.5">Passes needing immediate front-desk action</CardDescription>
              </div>
              <Button variant="ghost" size="sm" onClick={() => onNavigate('radar')} className="shrink-0 cursor-pointer">
                All ({attentionQueue.length})
              </Button>
            </div>

            <CardContent className="px-3 flex-1 divide-y">
              {attentionQueue.map((m) => {
                const isExpired = m.status !== 'Active'
                return (
                  <div
                    key={m.userId}
                    onClick={() => onSelectMember(m)}
                    className="flex items-center justify-between gap-3 px-2.5 py-3 rounded-lg hover:bg-muted/60 transition-colors cursor-pointer"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span
                          className={cn(
                            'h-2 w-2 rounded-full shrink-0',
                            isExpired ? 'bg-rose-500' : 'bg-amber-500'
                          )}
                        />
                        <p className="truncate text-sm font-medium">{m.fullName}</p>
                      </div>
                      <p className="mt-0.5 pl-4 font-mono text-[11px] text-muted-foreground tabular-nums">
                        {m.balanceLabel} · Ends {m.endDate}
                      </p>
                    </div>
                    <Button
                      size="xs"
                      variant="outline"
                      className="shrink-0 cursor-pointer"
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

              {attentionQueue.length === 0 && (
                <div className="py-16 text-center text-xs text-muted-foreground space-y-2">
                  <Progress value={100} className="w-24 mx-auto h-1.5 [&>div]:bg-emerald-500" />
                  <p>All active athlete passes are healthy.</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </BlurFade>
    </div>
  )
}

function MetricTile({ label, value, suffix, footnote, dotClass }) {
  return (
    <Card className="py-4 px-5 gap-2 shadow-none">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-medium uppercase tracking-[0.06em] text-muted-foreground">
          {label}
        </span>
        <span className={cn('h-2 w-2 rounded-full', dotClass)} />
      </div>
      <div className="flex items-baseline gap-2">
        <span className="text-3xl font-bold tracking-tight tabular-nums">
          <NumberFlow value={value} />
        </span>
        {suffix && <span className="font-mono text-xs text-muted-foreground">{suffix}</span>}
      </div>
      <p className="text-xs text-muted-foreground">{footnote}</p>
    </Card>
  )
}
