import React from 'react'
import NumberFlow from '@number-flow/react'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ArrowUpRight, CheckCircle2, Clock, Download, RefreshCw, UserPlus, Users, XCircle } from 'lucide-react'
import { Badge, Button, Card, cn } from './ui'

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
  const attentionQueue = members
    .filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7))
    .slice(0, 6)

  const chartData = [...(report?.monthlyRenewals || [])]
    .reverse()
    .map((item) => ({ month: item.month, renewals: item.count }))

  const activePct = members.length ? Math.round((activeCount / members.length) * 100) : 0

  return (
    <div className="space-y-6">
      {/* Top Hero Intro */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
            Live Operations Snapshot
          </p>
          <h1 className="mt-1 text-2xl sm:text-3xl font-bold text-zinc-900 dark:text-zinc-50">
            Front Desk Command Center
          </h1>
        </div>
        <div className="flex items-center gap-2.5">
          <a href="/api/reports/csv" download>
            <Button variant="secondary">
              <Download className="h-4 w-4" />
              Export CSV
            </Button>
          </a>
          <Button onClick={onNewMember}>
            <UserPlus className="h-4 w-4" />
            New Member
          </Button>
        </div>
      </div>

      {/* Bento Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Left 8 cols: Hero Stats + Recharts Area Chart */}
        <div className="lg:col-span-8 space-y-5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <BentoStat
              label="Total Roster"
              value={members.length}
              sub="Registered members"
              icon={Users}
              tone="neutral"
            />
            <BentoStat
              label="Active Passes"
              value={activeCount}
              sub={`${activePct}% pass health`}
              icon={CheckCircle2}
              tone="emerald"
            />
            <BentoStat
              label="Expiring ≤ 7d"
              value={expiringList.length}
              sub="Due soon"
              icon={Clock}
              tone="amber"
            />
            <BentoStat
              label="Expired"
              value={expiredCount}
              sub="Needs renewal"
              icon={XCircle}
              tone="rose"
            />
          </div>

          {/* Interactive Recharts Renewal Activity Chart */}
          <Card className="p-5">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-base font-semibold text-zinc-900 dark:text-zinc-50">
                  Monthly Pass Activations &amp; Renewals
                </h2>
                <p className="text-xs text-zinc-500">
                  Historical 1-month pass volume across the GymSync roster
                </p>
              </div>
              <Button variant="ghost" size="sm" onClick={() => onNavigate('directory')}>
                Full Directory
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Button>
            </div>

            <div className="h-64 w-full">
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData} margin={{ top: 10, right: 12, left: -18, bottom: 0 }}>
                    <defs>
                      <linearGradient id="emeraldFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#10b981" stopOpacity={0.28} />
                        <stop offset="65%" stopColor="#10b981" stopOpacity={0.08} />
                        <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(161,161,170,0.18)" />
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
                      strokeWidth={2.2}
                      fill="url(#emeraldFill)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-xs text-zinc-500">
                  No renewal data available yet.
                </div>
              )}
            </div>
          </Card>
        </div>

        {/* Right 4 cols: Live Action Queue */}
        <Card className="lg:col-span-4 p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-base font-semibold text-zinc-900 dark:text-zinc-50">
                  Needs Attention
                </h2>
                <p className="text-xs text-zinc-500">Expired or expiring within 7 days</p>
              </div>
              <Badge tone={attentionQueue.length > 0 ? 'amber' : 'emerald'}>
                {attentionQueue.length} queued
              </Badge>
            </div>

            <div className="divide-y divide-zinc-200 dark:divide-zinc-800">
              {attentionQueue.map((m) => {
                const isExp = m.status !== 'Active'
                return (
                  <div
                    key={m.userId}
                    className="py-3 flex items-center justify-between gap-2 group"
                  >
                    <button
                      onClick={() => onSelectMember(m)}
                      className="flex items-center gap-2.5 text-left min-w-0 cursor-pointer"
                    >
                      <span
                        className={cn(
                          'flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold border',
                          isExp
                            ? 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/60 dark:text-rose-400 dark:border-rose-900'
                            : 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/60 dark:text-amber-400 dark:border-amber-900'
                        )}
                      >
                        {m.initials}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-zinc-900 group-hover:text-emerald-600 dark:text-zinc-100">
                          {m.fullName}
                        </p>
                        <p className="text-xs text-zinc-500 tabular-nums">
                          {m.balanceLabel} • {m.endDate}
                        </p>
                      </div>
                    </button>
                    <Button variant="secondary" size="sm" onClick={() => onRenewMember(m)}>
                      <RefreshCw className="h-3 w-3" />
                      Renew
                    </Button>
                  </div>
                )
              })}
              {attentionQueue.length === 0 && (
                <p className="py-12 text-center text-xs text-zinc-500">
                  All active passes are healthy.
                </p>
              )}
            </div>
          </div>

          <Button variant="ghost" className="w-full mt-3" onClick={() => onNavigate('radar')}>
            View Full Expiry Radar
            <ArrowUpRight className="h-3.5 w-3.5" />
          </Button>
        </Card>
      </div>
    </div>
  )
}

function BentoStat({ label, value, sub, icon: Icon, tone }) {
  const iconWrap = {
    neutral: 'bg-zinc-100 text-zinc-600 dark:bg-zinc-900 dark:text-zinc-400',
    emerald: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400',
    amber: 'bg-amber-50 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400',
    rose: 'bg-rose-50 text-rose-600 dark:bg-rose-950/60 dark:text-rose-400',
  }
  return (
    <Card className="p-4 space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-zinc-500">{label}</span>
        <div className={cn('h-7 w-7 rounded-lg flex items-center justify-center', iconWrap[tone])}>
          <Icon className="h-3.5 w-3.5" />
        </div>
      </div>
      <div className="text-3xl font-bold tracking-tight tabular-nums">
        <NumberFlow value={value} />
      </div>
      <div className="text-xs text-zinc-500 dark:text-zinc-400">{sub}</div>
    </Card>
  )
}
