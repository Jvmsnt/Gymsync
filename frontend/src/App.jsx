import React, { useEffect, useMemo, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Bell,
  CheckCircle2,
  Download,
  Dumbbell,
  Eye,
  EyeOff,
  LogOut,
  Moon,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Shield,
  SlidersHorizontal,
  Sun,
  Trash2,
  UserPlus,
  Users,
  XCircle,
} from 'lucide-react'
import { Badge, Button, Card, Input, Modal, Select, cn } from './components/ui'
import { MusclePreviewCard } from './components/MuscleMap'

async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`)
  }
  return data
}

export default function App() {
  const [dark, setDark] = useState(false)
  const [user, setUser] = useState(null)
  const [health, setHealth] = useState({ jdbcConnected: false, isMySql: false, dbLabel: 'Memory DB' })
  const [activeTab, setActiveTab] = useState('checkin')
  const [toast, setToast] = useState(null)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
  }, [dark])

  useEffect(() => {
    api('/api/health').then(setHealth).catch(() => {})
  }, [])

  const notify = (message, tone = 'emerald') => {
    setToast({ message, tone, id: Date.now() })
    setTimeout(() => setToast((cur) => (cur && Date.now() - cur.id >= 3400 ? null : cur)), 3500)
  }

  const handleLoginSuccess = (authUser) => {
    setUser(authUser)
    setActiveTab('checkin')
  }

  if (!user) {
    return (
      <>
        <LoginView dark={dark} onToggleDark={() => setDark(!dark)} onSuccess={handleLoginSuccess} />
        <ToastBanner toast={toast} />
      </>
    )
  }

  const isAdmin = (user.role || '').toLowerCase() === 'admin'
  const navItems = isAdmin
    ? [
        { id: 'checkin', label: 'Member Check-In', icon: Users },
        { id: 'analytics', label: 'Analytics & Reports', icon: BarChart3 },
        { id: 'exercises', label: 'Exercise Library', icon: Dumbbell },
        { id: 'coach', label: 'Workout Coach', icon: SlidersHorizontal },
        { id: 'staff', label: 'Staff Accounts', icon: Shield },
        { id: 'alerts', label: 'Expiry Alerts', icon: Bell },
      ]
    : [
        { id: 'checkin', label: 'Member Check-In', icon: Users },
        { id: 'alerts', label: 'Expiry Alerts', icon: Bell },
        { id: 'coach', label: 'Workout Coach', icon: Dumbbell },
      ]

  const activeNav = navItems.find((n) => n.id === activeTab) || navItems[0]

  return (
    <div className="min-h-screen flex bg-zinc-50 text-zinc-950 dark:bg-zinc-950 dark:text-zinc-50">
      {/* Left Sidebar Rail */}
      <aside className="w-60 shrink-0 border-r border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950 flex flex-col justify-between">
        <div>
          {/* Brand Header */}
          <div className="flex items-center gap-2.5 border-b border-zinc-200 px-5 py-4 dark:border-zinc-800">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-600 text-white">
              <Dumbbell className="h-4 w-4" />
            </div>
            <div>
              <div className="text-sm font-bold tracking-tight text-zinc-900 dark:text-zinc-50">GYMSYNC</div>
              <div className="text-[11px] text-zinc-500 dark:text-zinc-400">
                {isAdmin ? 'Admin Console' : 'Front Desk'}
              </div>
            </div>
          </div>

          {/* Nav Group */}
          <div className="px-3 py-4">
            <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wider text-zinc-400">
              Operations
            </p>
            <nav className="space-y-1">
              {navItems.map((item) => {
                const Icon = item.icon
                const active = activeNav.id === item.id
                return (
                  <button
                    key={item.id}
                    onClick={() => setActiveTab(item.id)}
                    className={cn(
                      'w-full flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors cursor-pointer',
                      active
                        ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400'
                        : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100'
                    )}
                  >
                    <Icon className={cn('h-4 w-4', active ? 'text-emerald-600 dark:text-emerald-400' : 'text-zinc-400')} />
                    {item.label}
                  </button>
                )
              })}
            </nav>
          </div>
        </div>

        {/* Sidebar Footer */}
        <div className="border-t border-zinc-200 p-4 dark:border-zinc-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-zinc-900 dark:text-zinc-100">{user.fullName}</p>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 uppercase">{user.role}</p>
            </div>
            <Badge tone={health.jdbcConnected ? 'emerald' : 'neutral'}>
              <span className={cn('h-1.5 w-1.5 rounded-full', health.jdbcConnected ? 'bg-emerald-500' : 'bg-zinc-400')} />
              {health.dbLabel}
            </Badge>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <Button variant="secondary" size="sm" onClick={() => setDark(!dark)}>
              {dark ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
              {dark ? 'Light' : 'Dark'}
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setUser(null)}>
              <LogOut className="h-3.5 w-3.5" />
              Sign Out
            </Button>
          </div>
        </div>
      </aside>

      {/* Main Content Workspace */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="flex items-center justify-between border-b border-zinc-200 bg-white px-6 py-3.5 dark:border-zinc-800 dark:bg-zinc-950">
          <div className="flex items-center gap-2 text-sm">
            <span className="text-zinc-400">Workspace</span>
            <span className="text-zinc-300 dark:text-zinc-700">/</span>
            <span className="font-semibold text-zinc-900 dark:text-zinc-50">{activeNav.label}</span>
          </div>
          <div className="flex items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400">
            <Activity className="h-3.5 w-3.5 text-emerald-500" />
            <span>Java HTTP + {health.isMySql ? 'MySQL' : health.dbLabel} Connected</span>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">
          {activeNav.id === 'checkin' && <CheckInView isAdmin={isAdmin} notify={notify} />}
          {activeNav.id === 'analytics' && <AnalyticsView notify={notify} />}
          {activeNav.id === 'exercises' && <ExerciseLibraryView notify={notify} />}
          {activeNav.id === 'coach' && <WorkoutCoachView notify={notify} />}
          {activeNav.id === 'staff' && <StaffAccountsView currentUser={user} notify={notify} />}
          {activeNav.id === 'alerts' && <ExpiryAlertsView notify={notify} />}
        </main>
      </div>

      <ToastBanner toast={toast} />
    </div>
  )
}

function ToastBanner({ toast }) {
  if (!toast) return null
  return (
    <div className="fixed bottom-5 right-5 z-50 flex items-center gap-2.5 rounded-xl border border-zinc-200 bg-white px-4 py-3 text-sm font-medium text-zinc-900 shadow-lg dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-100">
      {toast.tone === 'rose' ? (
        <XCircle className="h-4 w-4 text-rose-500" />
      ) : (
        <CheckCircle2 className="h-4 w-4 text-emerald-500" />
      )}
      <span>{toast.message}</span>
    </div>
  )
}

/* ============================================================================
   1. SPLIT-SCREEN OPERATOR LOGIN
   ============================================================================ */
function LoginView({ dark, onToggleDark, onSuccess }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submitLogin = async (u, p) => {
    setError('')
    setLoading(true)
    try {
      const data = await api('/api/login', {
        method: 'POST',
        body: JSON.stringify({ username: u, password: p }),
      })
      onSuccess(data.user)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-white dark:bg-zinc-950">
      {/* Left Showcase */}
      <div className="hidden lg:flex flex-col justify-between bg-zinc-900 p-12 text-white border-r border-zinc-800">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-600 text-white">
            <Dumbbell className="h-5 w-5" />
          </div>
          <span className="text-lg font-bold tracking-tight">GYMSYNC</span>
        </div>

        <div className="max-w-md space-y-6">
          <Badge tone="emerald" className="bg-emerald-950 text-emerald-400 border-emerald-800">
            Front Desk Operations
          </Badge>
          <h1 className="text-4xl font-bold tracking-tight leading-tight">
            Gym Membership &amp; Workout Operations Console
          </h1>
          <p className="text-sm text-zinc-400 leading-relaxed">
            Built for front desk staff and gym administrators to verify passes, automate 1-month renewals, monitor expiries, and coach member workout plans.
          </p>
          <div className="space-y-3 pt-2 text-sm text-zinc-300">
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
              <span>Instant Active / Expired pass verification</span>
            </div>
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
              <span>Automated 1-month renewal date rules</span>
            </div>
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
              <span>Equipment-aware workout coaching &amp; muscle preview</span>
            </div>
          </div>
        </div>

        <p className="text-xs text-zinc-500">DIT 2-3 Group 3 • Integrative Programming (Java + MySQL + React)</p>
      </div>

      {/* Right Sign-In Form */}
      <div className="flex flex-col justify-between p-8 sm:p-12">
        <div className="flex justify-end">
          <Button variant="secondary" size="sm" onClick={onToggleDark}>
            {dark ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
            {dark ? 'Light Mode' : 'Dark Mode'}
          </Button>
        </div>

        <div className="mx-auto w-full max-w-sm space-y-6">
          <div>
            <h2 className="text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">Sign in</h2>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
              Enter your Admin or Front Desk Staff credentials.
            </p>
          </div>

          {error && (
            <div className="rounded-lg border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-xs font-medium text-rose-700 dark:border-rose-900 dark:bg-rose-950/60 dark:text-rose-400">
              {error}
            </div>
          )}

          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              submitLogin(username.trim(), password)
            }}
          >
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">Username</label>
              <Input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. admin or staff"
                autoFocus
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">Password</label>
              <div className="relative">
                <Input
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  className="pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-2.5 top-2.5 text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 cursor-pointer"
                >
                  {showPass ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" size="lg" className="w-full" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign in to Console'}
            </Button>
          </form>

          <div className="border-t border-zinc-200 dark:border-zinc-800 pt-5 space-y-2.5">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-zinc-400">
              Quick Demo Operator Access
            </p>
            <div className="grid grid-cols-2 gap-2.5">
              <Button
                variant="secondary"
                onClick={() => {
                  setUsername('admin')
                  setPassword('admin123')
                  submitLogin('admin', 'admin123')
                }}
              >
                <Shield className="h-3.5 w-3.5 text-zinc-500" />
                Admin Console
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setUsername('staff')
                  setPassword('staff123')
                  submitLogin('staff', 'staff123')
                }}
              >
                <Users className="h-3.5 w-3.5 text-emerald-600" />
                Front Desk Staff
              </Button>
            </div>
          </div>
        </div>

        <div />
      </div>
    </div>
  )
}

/* ============================================================================
   2. FRONT DESK CHECK-IN & MEMBER INSPECTOR
   ============================================================================ */
function CheckInView({ isAdmin, notify }) {
  const [members, setMembers] = useState([])
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all')
  const [selectedId, setSelectedId] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingMember, setEditingMember] = useState(null)

  const load = async () => {
    try {
      const data = await api('/api/members')
      setMembers(data.members || [])
      if (!selectedId && data.members?.length) {
        setSelectedId(data.members[0].userId)
      }
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const stats = useMemo(() => {
    const total = members.length
    const active = members.filter((m) => m.status === 'Active').length
    const expiring = members.filter((m) => m.status === 'Active' && m.daysRemaining >= 0 && m.daysRemaining <= 7).length
    const expired = members.filter((m) => m.status !== 'Active').length
    return { total, active, expiring, expired }
  }, [members])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return members.filter((m) => {
      if (filter === 'active' && m.status !== 'Active') return false
      if (filter === 'expired' && m.status === 'Active') return false
      if (filter === 'expiring' && !(m.status === 'Active' && m.daysRemaining >= 0 && m.daysRemaining <= 7)) return false
      if (!q) return true
      return (
        m.fullName.toLowerCase().includes(q) ||
        m.username.toLowerCase().includes(q) ||
        (m.contactNo || '').toLowerCase().includes(q) ||
        (m.fitnessLevel || '').toLowerCase().includes(q)
      )
    })
  }, [members, search, filter])

  const selected = useMemo(
    () => filtered.find((m) => m.userId === selectedId) || filtered[0] || null,
    [filtered, selectedId]
  )

  const renewMember = async (m) => {
    try {
      const res = await api(`/api/members/${m.userId}/renew`, { method: 'POST' })
      notify(`Renewed ${m.fullName} through ${res.endDate}`)
      await load()
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  const deleteMember = async (m) => {
    if (!window.confirm(`Delete member ${m.fullName} (#${m.userId}) and all records?`)) return
    try {
      await api(`/api/members/${m.userId}`, { method: 'DELETE' })
      notify(`Deleted member ${m.fullName}`)
      setSelectedId(null)
      await load()
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  return (
    <div className="space-y-5">
      {/* KPI Strip */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard title="Total Members" value={stats.total} sub="Registered athletes" icon={Users} tone="neutral" />
        <KpiCard title="Active Passes" value={stats.active} sub="Cleared for entry" icon={CheckCircle2} tone="emerald" />
        <KpiCard title="Expiring ≤ 7 Days" value={stats.expiring} sub="Due for renewal" icon={AlertTriangle} tone="amber" />
        <KpiCard title="Expired" value={stats.expired} sub="Renewal required" icon={XCircle} tone="rose" />
      </div>

      {/* Toolbar */}
      <Card className="p-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2.5 flex-1">
          <div className="relative w-72 max-w-full">
            <Search className="h-4 w-4 text-zinc-400 absolute left-3 top-2.5" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search name, @username, phone..."
              className="pl-9"
            />
          </div>

          <div className="inline-flex rounded-lg bg-zinc-100 p-0.5 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800">
            {[
              { id: 'all', label: 'All' },
              { id: 'active', label: 'Active' },
              { id: 'expiring', label: 'Expiring' },
              { id: 'expired', label: 'Expired' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setFilter(tab.id)}
                className={cn(
                  'rounded-md px-3 py-1 text-xs font-medium transition-colors cursor-pointer',
                  filter === tab.id
                    ? 'bg-white text-zinc-900 shadow-2xs dark:bg-zinc-800 dark:text-zinc-50'
                    : 'text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-200'
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <Button
          onClick={() => {
            setEditingMember(null)
            setModalOpen(true)
          }}
        >
          <UserPlus className="h-4 w-4" />
          New Member
        </Button>
      </Card>

      {/* Master-Detail Split */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <Card className="xl:col-span-8 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
                  <th className="py-3 px-4">ID</th>
                  <th className="py-3 px-4">Member</th>
                  <th className="py-3 px-4">Contact</th>
                  <th className="py-3 px-4">Level</th>
                  <th className="py-3 px-4">End Date</th>
                  <th className="py-3 px-4">Balance</th>
                  <th className="py-3 px-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
                {filtered.map((m) => {
                  const isSel = selected && selected.userId === m.userId
                  const isAct = m.status === 'Active'
                  const isExpSoon = isAct && m.daysRemaining >= 0 && m.daysRemaining <= 7
                  return (
                    <tr
                      key={m.userId}
                      onClick={() => setSelectedId(m.userId)}
                      className={cn(
                        'cursor-pointer transition-colors',
                        isSel
                          ? 'bg-emerald-50/70 dark:bg-emerald-950/30'
                          : 'hover:bg-zinc-50 dark:hover:bg-zinc-900/60'
                      )}
                    >
                      <td className="py-3 px-4 font-mono text-xs text-zinc-500">#{m.userId}</td>
                      <td className="py-3 px-4">
                        <div className="font-medium text-zinc-900 dark:text-zinc-100">{m.fullName}</div>
                        <div className="text-xs text-zinc-500">@{m.username}</div>
                      </td>
                      <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{m.contactNo}</td>
                      <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{m.fitnessLevel}</td>
                      <td className="py-3 px-4 font-mono text-xs text-zinc-600 dark:text-zinc-300">{m.endDate}</td>
                      <td className="py-3 px-4 text-xs font-medium text-zinc-700 dark:text-zinc-300">{m.balanceLabel}</td>
                      <td className="py-3 px-4">
                        <Badge tone={isAct ? (isExpSoon ? 'amber' : 'emerald') : 'rose'}>
                          <span
                            className={cn(
                              'h-1.5 w-1.5 rounded-full',
                              isAct ? (isExpSoon ? 'bg-amber-500' : 'bg-emerald-500') : 'bg-rose-500'
                            )}
                          />
                          {isAct ? (isExpSoon ? 'Expiring' : 'Active') : 'Expired'}
                        </Badge>
                      </td>
                    </tr>
                  )
                })}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={7} className="py-10 text-center text-sm text-zinc-500">
                      No members match your filter.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>

        {/* Interactive Member Inspector */}
        <Card className="xl:col-span-4 p-5 space-y-5">
          {!selected ? (
            <div className="py-16 text-center text-sm text-zinc-500">Select a member row to inspect details.</div>
          ) : (
            <>
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div
                    className={cn(
                      'flex h-11 w-11 items-center justify-center rounded-full text-sm font-bold border',
                      selected.status === 'Active'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-400 dark:border-emerald-900'
                        : 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/60 dark:text-rose-400 dark:border-rose-900'
                    )}
                  >
                    {selected.initials}
                  </div>
                  <div>
                    <h3 className="text-base font-semibold text-zinc-900 dark:text-zinc-50">{selected.fullName}</h3>
                    <p className="text-xs text-zinc-500">@{selected.username} • Member #{selected.userId}</p>
                  </div>
                </div>
                <Badge tone={selected.status === 'Active' ? 'emerald' : 'rose'}>
                  {selected.status === 'Active' ? 'Entry Cleared' : 'Expired'}
                </Badge>
              </div>

              {/* Cycle Progress */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold uppercase tracking-wider text-zinc-400 text-[10px]">
                    Cycle Progress
                  </span>
                  <span
                    className={cn(
                      'font-semibold',
                      selected.status === 'Active' ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
                    )}
                  >
                    {selected.balanceLabel}
                  </span>
                </div>
                <div className="h-2 w-full rounded-full bg-zinc-100 dark:bg-zinc-900 overflow-hidden">
                  <div
                    className={cn(
                      'h-full rounded-full transition-all',
                      selected.status === 'Active' ? 'bg-emerald-500' : 'bg-rose-500'
                    )}
                    style={{
                      width: `${
                        selected.status === 'Active'
                          ? Math.max(8, Math.min(100, (selected.daysRemaining / 30) * 100))
                          : 100
                      }%`,
                    }}
                  />
                </div>
              </div>

              {/* Hairline Metadata */}
              <div className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
                <div className="grid grid-cols-2 gap-4 py-2.5">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Fitness Level</p>
                    <p className="mt-0.5 font-medium">{selected.fitnessLevel}</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Gender</p>
                    <p className="mt-0.5 font-medium">{selected.gender || 'Unspecified'}</p>
                  </div>
                </div>
                <div className="py-2.5">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Contact &amp; Address</p>
                  <p className="mt-0.5 font-medium">
                    {selected.contactNo}
                    {selected.address ? ` • ${selected.address}` : ''}
                  </p>
                </div>
                <div className="py-2.5">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Emergency Contact</p>
                  <p className="mt-0.5 font-medium">
                    {selected.emergencyContactName || 'N/A'} ({selected.emergencyContactNo || 'N/A'})
                  </p>
                </div>
                <div className="py-2.5">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Current 1-Month Period</p>
                  <p className="mt-0.5 font-mono text-xs">{selected.startDate} → {selected.endDate}</p>
                </div>
                <div className="py-2.5">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Next Renewal Window</p>
                  <p className="mt-0.5 font-mono text-xs text-emerald-600 dark:text-emerald-400">
                    {selected.nextRenewalStart} → {selected.nextRenewalEnd}
                  </p>
                </div>
              </div>

              {/* Embedded Quick Actions */}
              <div className="space-y-2 pt-1">
                <div className="grid grid-cols-2 gap-2">
                  <Button onClick={() => renewMember(selected)}>
                    <RefreshCw className="h-3.5 w-3.5" />
                    Renew +1 Mo
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setEditingMember(selected)
                      setModalOpen(true)
                    }}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                    Edit Profile
                  </Button>
                </div>
                {isAdmin && (
                  <Button variant="danger" className="w-full" onClick={() => deleteMember(selected)}>
                    <Trash2 className="h-3.5 w-3.5" />
                    Delete Member
                  </Button>
                )}
              </div>
            </>
          )}
        </Card>
      </div>

      <MemberFormModal
        open={modalOpen}
        member={editingMember}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false)
          load()
        }}
        notify={notify}
      />
    </div>
  )
}

function KpiCard({ title, value, sub, icon: Icon, tone }) {
  const iconColors = {
    neutral: 'text-zinc-500 bg-zinc-100 dark:bg-zinc-900',
    emerald: 'text-emerald-600 bg-emerald-50 dark:bg-emerald-950/60 dark:text-emerald-400',
    amber: 'text-amber-600 bg-amber-50 dark:bg-amber-950/60 dark:text-amber-400',
    rose: 'text-rose-600 bg-rose-50 dark:bg-rose-950/60 dark:text-rose-400',
  }
  return (
    <Card className="p-4 flex flex-col justify-between gap-2">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-zinc-500">{title}</span>
        <div className={cn('h-7 w-7 rounded-lg flex items-center justify-center', iconColors[tone])}>
          <Icon className="h-3.5 w-3.5" />
        </div>
      </div>
      <div className="text-2xl font-bold tracking-tight">{value}</div>
      <div className="text-xs text-zinc-500 dark:text-zinc-400">{sub}</div>
    </Card>
  )
}

function MemberFormModal({ open, member, onClose, onSaved, notify }) {
  const isNew = !member
  const [form, setForm] = useState({})

  useEffect(() => {
    if (member) {
      setForm({
        username: member.username || '',
        password: '',
        firstName: member.firstName || '',
        lastName: member.lastName || '',
        contactNo: member.contactNo || '09',
        emergencyContactName: member.emergencyContactName || '',
        emergencyContactNo: member.emergencyContactNo || '09',
        fitnessLevel: member.fitnessLevel || 'Beginner',
        gender: member.gender || 'Male',
        birthdate: member.birthdate || '',
        address: member.address || '',
      })
    } else {
      setForm({
        username: '',
        password: '',
        firstName: '',
        lastName: '',
        contactNo: '09',
        emergencyContactName: '',
        emergencyContactNo: '09',
        fitnessLevel: 'Beginner',
        gender: 'Male',
        birthdate: '',
        address: '',
      })
    }
  }, [member, open])

  const submit = async (e) => {
    e.preventDefault()
    try {
      if (isNew) {
        await api('/api/members', { method: 'POST', body: JSON.stringify(form) })
        notify(`Registered ${form.firstName} ${form.lastName} with 1-month pass`)
      } else {
        await api(`/api/members/${member.userId}`, { method: 'PUT', body: JSON.stringify(form) })
        notify(`Updated profile for ${form.firstName} ${form.lastName}`)
      }
      onSaved()
    } catch (err) {
      notify(err.message, 'rose')
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isNew ? 'Register New Member' : `Edit Member — ${member?.fullName}`}
      subtitle={isNew ? 'Automatically activates a 1-month gym pass from today.' : 'Update athlete profile details.'}
      maxWidth="max-w-xl"
    >
      <form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Username *</label>
            <Input className="mt-1" value={form.username || ''} onChange={(e) => setForm({ ...form, username: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">
              {isNew ? 'Password *' : 'New Password (optional)'}
            </label>
            <Input
              type="password"
              className="mt-1"
              value={form.password || ''}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">First Name *</label>
            <Input className="mt-1" value={form.firstName || ''} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Last Name *</label>
            <Input className="mt-1" value={form.lastName || ''} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Contact No. *</label>
            <Input className="mt-1" value={form.contactNo || ''} onChange={(e) => setForm({ ...form, contactNo: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Fitness Level *</label>
            <Select className="mt-1" value={form.fitnessLevel || 'Beginner'} onChange={(e) => setForm({ ...form, fitnessLevel: e.target.value })}>
              <option>Beginner</option>
              <option>Intermediate</option>
              <option>Advanced</option>
            </Select>
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Emergency Contact Name *</label>
            <Input
              className="mt-1"
              value={form.emergencyContactName || ''}
              onChange={(e) => setForm({ ...form, emergencyContactName: e.target.value })}
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Emergency Contact No. *</label>
            <Input
              className="mt-1"
              value={form.emergencyContactNo || ''}
              onChange={(e) => setForm({ ...form, emergencyContactNo: e.target.value })}
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Gender</label>
            <Select className="mt-1" value={form.gender || 'Male'} onChange={(e) => setForm({ ...form, gender: e.target.value })}>
              <option>Male</option>
              <option>Female</option>
              <option>Other</option>
              <option>Prefer not to say</option>
            </Select>
          </div>
          <div>
            <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Birthdate (YYYY-MM-DD)</label>
            <Input
              className="mt-1"
              placeholder="2002-05-15"
              value={form.birthdate || ''}
              onChange={(e) => setForm({ ...form, birthdate: e.target.value })}
            />
          </div>
        </div>
        <div>
          <label className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Address (optional)</label>
          <Input className="mt-1" value={form.address || ''} onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit">{isNew ? 'Activate Member Pass' : 'Save Changes'}</Button>
        </div>
      </form>
    </Modal>
  )
}

/* ============================================================================
   3. ANALYTICS & REPORTS
   ============================================================================ */
function AnalyticsView({ notify }) {
  const [report, setReport] = useState(null)

  const load = async () => {
    try {
      setReport(await api('/api/reports'))
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  useEffect(() => {
    load()
  }, [])

  if (!report) return <div className="text-sm text-zinc-500">Loading analytics...</div>

  const maxMonthly = Math.max(1, ...(report.monthlyRenewals || []).map((r) => r.count))

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Live Membership Analytics</h2>
          <p className="text-xs text-zinc-500">Real-time summary from GymSync repository</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={load}>
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </Button>
          <a href="/api/reports/csv" download>
            <Button>
              <Download className="h-3.5 w-3.5" />
              Export CSV
            </Button>
          </a>
        </div>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard title="Total Members" value={report.totalMembers} sub="Registered accounts" icon={Users} tone="neutral" />
        <KpiCard title="Active Passes" value={report.activeCount} sub="Cleared for entry" icon={CheckCircle2} tone="emerald" />
        <KpiCard title="Expired Passes" value={report.expiredCount} sub="Needs renewal" icon={XCircle} tone="rose" />
        <KpiCard title="Expiring ≤ 7d" value={report.expiring7Count} sub="Upcoming renewals" icon={AlertTriangle} tone="amber" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <Card className="p-5 space-y-4">
          <h3 className="text-sm font-semibold">1. Active vs. Expired Summary</h3>
          <div className="space-y-3 text-sm">
            <div>
              <div className="flex justify-between text-xs mb-1">
                <span className="font-medium text-emerald-600">Active Passes</span>
                <span>{report.activeCount} ({report.activeShare})</span>
              </div>
              <div className="h-2 rounded-full bg-zinc-100 dark:bg-zinc-900 overflow-hidden">
                <div className="h-full bg-emerald-500 rounded-full" style={{ width: report.activeShare }} />
              </div>
            </div>
            <div>
              <div className="flex justify-between text-xs mb-1">
                <span className="font-medium text-rose-600">Expired Passes</span>
                <span>{report.expiredCount} ({report.expiredShare})</span>
              </div>
              <div className="h-2 rounded-full bg-zinc-100 dark:bg-zinc-900 overflow-hidden">
                <div className="h-full bg-rose-500 rounded-full" style={{ width: report.expiredShare }} />
              </div>
            </div>
          </div>
        </Card>

        <Card className="p-5 space-y-3">
          <h3 className="text-sm font-semibold">2. Expiring Within 7 Days</h3>
          <div className="divide-y divide-zinc-200 dark:divide-zinc-800 text-xs">
            {(report.expiringSoon || []).map((r, i) => (
              <div key={i} className="flex items-center justify-between py-2">
                <div>
                  <p className="font-medium text-zinc-900 dark:text-zinc-100">{r.name}</p>
                  <p className="text-zinc-500">{r.endDate}</p>
                </div>
                <Badge tone="amber">{r.days}d left</Badge>
              </div>
            ))}
            {(report.expiringSoon || []).length === 0 && (
              <p className="text-zinc-500 py-4">No memberships expiring in the next 7 days.</p>
            )}
          </div>
        </Card>

        <Card className="p-5 space-y-3">
          <h3 className="text-sm font-semibold">3. Monthly Renewals Log</h3>
          <div className="space-y-2.5 text-xs">
            {(report.monthlyRenewals || []).map((r) => (
              <div key={r.month} className="space-y-1">
                <div className="flex justify-between">
                  <span className="font-mono font-medium">{r.month}</span>
                  <span className="font-semibold">{r.count}</span>
                </div>
                <div className="h-1.5 rounded-full bg-zinc-100 dark:bg-zinc-900 overflow-hidden">
                  <div className="h-full bg-emerald-600 rounded-full" style={{ width: `${(r.count / maxMonthly) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}

/* ============================================================================
   4. EXERCISE LIBRARY
   ============================================================================ */
function ExerciseLibraryView({ notify }) {
  const [exercises, setExercises] = useState([])
  const [showMachines, setShowMachines] = useState(true)
  const [showFree, setShowFree] = useState(true)
  const [selectedId, setSelectedId] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const load = async () => {
    try {
      const data = await api('/api/exercises')
      setExercises(data.exercises || [])
      if (!selectedId && data.exercises?.length) setSelectedId(data.exercises[0].exerciseId)
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filtered = exercises.filter((ex) => {
    const isMach = (ex.equipment || '').toLowerCase().includes('machine')
    if (isMach && !showMachines) return false
    if (!isMach && !showFree) return false
    return true
  })

  const selected = filtered.find((e) => e.exerciseId === selectedId) || filtered[0] || null

  const remove = async (ex) => {
    if (!window.confirm(`Delete exercise '${ex.name}'?`)) return
    try {
      await api(`/api/exercises/${ex.exerciseId}`, { method: 'DELETE' })
      notify(`Deleted ${ex.name}`)
      await load()
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  return (
    <div className="space-y-5">
      <Card className="p-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
            <input type="checkbox" checked={showMachines} onChange={(e) => setShowMachines(e.target.checked)} className="accent-emerald-600" />
            Show Gym Machine Workouts
          </label>
          <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
            <input type="checkbox" checked={showFree} onChange={(e) => setShowFree(e.target.checked)} className="accent-emerald-600" />
            Show Free-Weight / Bodyweight
          </label>
        </div>
        <div className="flex gap-2">
          <Button
            variant="secondary"
            size="sm"
            disabled={!selected}
            onClick={() => {
              setEditing(selected)
              setModalOpen(true)
            }}
          >
            <Pencil className="h-3.5 w-3.5" />
            Edit
          </Button>
          <Button variant="danger" size="sm" disabled={!selected} onClick={() => selected && remove(selected)}>
            <Trash2 className="h-3.5 w-3.5" />
            Delete
          </Button>
          <Button
            size="sm"
            onClick={() => {
              setEditing(null)
              setModalOpen(true)
            }}
          >
            <Plus className="h-3.5 w-3.5" />
            Add Exercise
          </Button>
        </div>
      </Card>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <Card className="xl:col-span-8 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
                  <th className="py-3 px-4">ID</th>
                  <th className="py-3 px-4">Workout Name</th>
                  <th className="py-3 px-4">Target Muscle</th>
                  <th className="py-3 px-4">Level</th>
                  <th className="py-3 px-4">Equipment</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
                {filtered.map((ex) => {
                  const isMach = (ex.equipment || '').toLowerCase().includes('machine')
                  return (
                    <tr
                      key={ex.exerciseId}
                      onClick={() => setSelectedId(ex.exerciseId)}
                      className={cn(
                        'cursor-pointer transition-colors',
                        selected?.exerciseId === ex.exerciseId
                          ? 'bg-emerald-50/70 dark:bg-emerald-950/30'
                          : 'hover:bg-zinc-50 dark:hover:bg-zinc-900/60'
                      )}
                    >
                      <td className="py-3 px-4 font-mono text-xs text-zinc-500">#{ex.exerciseId}</td>
                      <td className="py-3 px-4 font-medium">{ex.name}</td>
                      <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{ex.muscleGroup}</td>
                      <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{ex.difficulty}</td>
                      <td className="py-3 px-4">
                        <Badge tone={isMach ? 'blue' : 'emerald'}>
                          {ex.equipment.replace('Machine: ', '')}
                        </Badge>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </Card>

        <div className="xl:col-span-4">
          <MusclePreviewCard exercise={selected} />
        </div>
      </div>

      <ExerciseModal
        open={modalOpen}
        exercise={editing}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false)
          load()
        }}
        notify={notify}
      />
    </div>
  )
}

function ExerciseModal({ open, exercise, onClose, onSaved, notify }) {
  const isEdit = !!exercise
  const [form, setForm] = useState({})

  useEffect(() => {
    setForm(
      exercise
        ? { ...exercise }
        : { name: '', muscleGroup: 'Chest', difficulty: 'Beginner', equipment: 'Machine: Lat Pulldown Station', description: '' }
    )
  }, [exercise, open])

  const submit = async (e) => {
    e.preventDefault()
    try {
      if (isEdit) {
        await api(`/api/exercises/${exercise.exerciseId}`, { method: 'PUT', body: JSON.stringify(form) })
        notify(`Updated ${form.name}`)
      } else {
        await api('/api/exercises', { method: 'POST', body: JSON.stringify(form) })
        notify(`Added ${form.name}`)
      }
      onSaved()
    } catch (err) {
      notify(err.message, 'rose')
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Edit Exercise' : 'Add Exercise'}>
      <form onSubmit={submit} className="space-y-3.5">
        <div>
          <label className="text-xs font-semibold">Workout Name *</label>
          <Input className="mt-1" value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-semibold">Muscle Group</label>
            <Select className="mt-1" value={form.muscleGroup || 'Chest'} onChange={(e) => setForm({ ...form, muscleGroup: e.target.value })}>
              {['Chest', 'Back', 'Legs', 'Shoulders', 'Arms', 'Core', 'Full Body'].map((g) => (
                <option key={g}>{g}</option>
              ))}
            </Select>
          </div>
          <div>
            <label className="text-xs font-semibold">Difficulty</label>
            <Select className="mt-1" value={form.difficulty || 'Beginner'} onChange={(e) => setForm({ ...form, difficulty: e.target.value })}>
              {['Beginner', 'Intermediate', 'Advanced'].map((d) => (
                <option key={d}>{d}</option>
              ))}
            </Select>
          </div>
        </div>
        <div>
          <label className="text-xs font-semibold">Equipment (prefix with &quot;Machine: &quot; if gym machine)</label>
          <Input className="mt-1" value={form.equipment || ''} onChange={(e) => setForm({ ...form, equipment: e.target.value })} />
        </div>
        <div>
          <label className="text-xs font-semibold">Execution Cues *</label>
          <textarea
            rows={3}
            className="mt-1 w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950"
            value={form.description || ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit">Save Exercise</Button>
        </div>
      </form>
    </Modal>
  )
}

/* ============================================================================
   5. WORKOUT COACH (Smart Generator + Saved Plans + Muscle Preview)
   ============================================================================ */
function WorkoutCoachView({ notify }) {
  const [members, setMembers] = useState([])
  const [memberId, setMemberId] = useState('')
  const [plans, setPlans] = useState([])
  const [planId, setPlanId] = useState('')
  const [exercises, setExercises] = useState([])
  const [selectedEx, setSelectedEx] = useState(null)

  const [goal, setGoal] = useState('General Fitness')
  const [level, setLevel] = useState('Beginner')
  const [days, setDays] = useState(3)
  const [zone, setZone] = useState('Full Gym')

  useEffect(() => {
    Promise.all([api('/api/members'), api('/api/exercises')]).then(([mRes, eRes]) => {
      const list = mRes.members || []
      setMembers(list)
      setExercises(eRes.exercises || [])
      if (list.length) {
        setMemberId(String(list[0].userId))
        setLevel(list[0].fitnessLevel || 'Beginner')
      }
    })
  }, [])

  const loadPlans = async (uid) => {
    if (!uid) return
    const res = await api(`/api/workout-plans?userId=${uid}`)
    const list = res.plans || []
    setPlans(list)
    if (list.length) {
      setPlanId(String(list[0].workoutPlanId))
    } else {
      setPlanId('')
      setSelectedEx(null)
    }
  }

  useEffect(() => {
    loadPlans(memberId)
  }, [memberId])

  const activePlan = plans.find((p) => String(p.workoutPlanId) === String(planId)) || plans[0] || null

  const generate = async () => {
    try {
      const res = await api('/api/workout-plans/generate', {
        method: 'POST',
        body: JSON.stringify({
          userId: Number(memberId),
          goal,
          fitnessLevel: level,
          daysPerWeek: Number(days),
          equipmentPref: zone,
          includeMachines: zone === 'Full Gym' || zone === 'Gym Machines Only',
        }),
      })
      notify(`Generated routine: ${res.plan.goal}`)
      await loadPlans(memberId)
      setPlanId(String(res.plan.workoutPlanId))
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  const deletePlan = async () => {
    if (!activePlan) return
    try {
      await api(`/api/workout-plans/${activePlan.workoutPlanId}`, { method: 'DELETE' })
      notify(`Deleted plan #${activePlan.workoutPlanId}`)
      await loadPlans(memberId)
    } catch (e) {
      notify(e.message, 'rose')
    }
  }

  return (
    <div className="space-y-5">
      <Card className="p-4 flex flex-wrap items-end gap-3">
        <div className="w-64">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Select Member</label>
          <Select className="mt-1" value={memberId} onChange={(e) => setMemberId(e.target.value)}>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                #{m.userId} — {m.fullName} ({m.fitnessLevel})
              </option>
            ))}
          </Select>
        </div>
        <div className="w-40">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Goal</label>
          <Select className="mt-1" value={goal} onChange={(e) => setGoal(e.target.value)}>
            {['Weight Loss', 'Muscle Gain', 'General Fitness', 'Strength Foundation'].map((g) => (
              <option key={g}>{g}</option>
            ))}
          </Select>
        </div>
        <div className="w-36">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Level</label>
          <Select className="mt-1" value={level} onChange={(e) => setLevel(e.target.value)}>
            {['Beginner', 'Intermediate', 'Advanced'].map((l) => (
              <option key={l}>{l}</option>
            ))}
          </Select>
        </div>
        <div className="w-24">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Days/Wk</label>
          <Select className="mt-1" value={days} onChange={(e) => setDays(e.target.value)}>
            {[2, 3, 4, 5].map((d) => (
              <option key={d} value={d}>{d} days</option>
            ))}
          </Select>
        </div>
        <div className="w-44">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Equipment Zone</label>
          <Select className="mt-1" value={zone} onChange={(e) => setZone(e.target.value)}>
            {['Full Gym', 'Gym Machines Only', 'Dumbbells', 'Bodyweight'].map((z) => (
              <option key={z}>{z}</option>
            ))}
          </Select>
        </div>
        <Button onClick={generate}>
          <Plus className="h-4 w-4" />
          Generate Plan
        </Button>
      </Card>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <div className="xl:col-span-8 space-y-3">
          <Card className="p-3 flex items-center justify-between gap-3">
            <div className="flex-1">
              <Select value={planId} onChange={(e) => setPlanId(e.target.value)} disabled={plans.length === 0}>
                {plans.length === 0 && <option>No saved plans yet</option>}
                {plans.map((p) => (
                  <option key={p.workoutPlanId} value={p.workoutPlanId}>
                    Plan #{p.workoutPlanId} [{p.planType}] — {p.goal}
                  </option>
                ))}
              </Select>
            </div>
            <Button variant="danger" size="sm" disabled={!activePlan} onClick={deletePlan}>
              <Trash2 className="h-3.5 w-3.5" />
              Delete Plan
            </Button>
          </Card>

          <Card className="overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
                  <th className="py-3 px-4">Day</th>
                  <th className="py-3 px-4">Exercise</th>
                  <th className="py-3 px-4">Muscle</th>
                  <th className="py-3 px-4">Equipment</th>
                  <th className="py-3 px-4">Sets × Reps</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
                {(activePlan?.exercises || []).map((item, idx) => {
                  const fullEx = exercises.find((e) => e.name.toLowerCase() === item.exerciseName.toLowerCase()) || null
                  return (
                    <tr
                      key={idx}
                      onClick={() => setSelectedEx(fullEx)}
                      className="cursor-pointer hover:bg-zinc-50 dark:hover:bg-zinc-900/60"
                    >
                      <td className="py-3 px-4 font-medium">{item.dayOfWeek}</td>
                      <td className="py-3 px-4 font-medium">{item.exerciseName}</td>
                      <td className="py-3 px-4 text-zinc-500">{item.muscleGroup}</td>
                      <td className="py-3 px-4">
                        <Badge tone={(item.equipment || '').toLowerCase().includes('machine') ? 'blue' : 'emerald'}>
                          {(item.equipment || '').replace('Machine: ', '')}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 font-mono text-xs">{item.sets} × {item.reps}</td>
                    </tr>
                  )
                })}
                {(!activePlan || activePlan.exercises.length === 0) && (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm text-zinc-500">
                      Generate a workout plan above to see exercises.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </Card>
        </div>

        <div className="xl:col-span-4">
          <MusclePreviewCard exercise={selectedEx || (activePlan?.exercises?.[0] ? exercises.find((e) => e.name === activePlan.exercises[0].exerciseName) : null)} />
        </div>
      </div>
    </div>
  )
}

/* ============================================================================
   6. STAFF ACCOUNTS & EXPIRY ALERTS
   ============================================================================ */
function StaffAccountsView({ currentUser, notify }) {
  const [users, setUsers] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', role: 'Staff', firstName: '', lastName: '', contactNo: '09' })

  const load = async () => {
    const res = await api('/api/staff')
    setUsers(res.users || [])
  }

  useEffect(() => {
    load()
  }, [])

  const create = async (e) => {
    e.preventDefault()
    try {
      await api('/api/staff', { method: 'POST', body: JSON.stringify(form) })
      notify(`Created account @${form.username}`)
      setOpen(false)
      setForm({ username: '', password: '', role: 'Staff', firstName: '', lastName: '', contactNo: '09' })
      load()
    } catch (err) {
      notify(err.message, 'rose')
    }
  }

  const remove = async (u) => {
    if (u.userId === currentUser.userId) return notify('Cannot delete your own active Admin account.', 'rose')
    if (!window.confirm(`Delete @${u.username}?`)) return
    await api(`/api/staff/${u.userId}`, { method: 'DELETE' })
    notify(`Deleted @${u.username}`)
    load()
  }

  return (
    <div className="space-y-4">
      <Card className="p-3 flex justify-between items-center">
        <p className="text-sm text-zinc-500 px-2">Manage all registered Admin, Front Desk Staff, and Member accounts.</p>
        <Button onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4" />
          Add Admin / Staff
        </Button>
      </Card>

      <Card className="overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
              <th className="py-3 px-4">ID</th>
              <th className="py-3 px-4">Name</th>
              <th className="py-3 px-4">Username</th>
              <th className="py-3 px-4">Role</th>
              <th className="py-3 px-4">Contact</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
            {users.map((u) => (
              <tr key={u.userId} className="hover:bg-zinc-50 dark:hover:bg-zinc-900/60">
                <td className="py-3 px-4 font-mono text-xs text-zinc-500">#{u.userId}</td>
                <td className="py-3 px-4 font-medium">{u.fullName}</td>
                <td className="py-3 px-4 text-zinc-500">@{u.username}</td>
                <td className="py-3 px-4">
                  <Badge tone={u.role === 'Admin' ? 'blue' : u.role === 'Staff' ? 'emerald' : 'neutral'}>{u.role}</Badge>
                </td>
                <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{u.contactNo}</td>
                <td className="py-3 px-4 text-right">
                  <Button variant="danger" size="sm" onClick={() => remove(u)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Modal open={open} onClose={() => setOpen(false)} title="Create Operator Account">
        <form onSubmit={create} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-semibold">Username *</label>
              <Input className="mt-1" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-semibold">Password *</label>
              <Input type="password" className="mt-1" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-semibold">First Name *</label>
              <Input className="mt-1" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-semibold">Last Name *</label>
              <Input className="mt-1" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-semibold">Role</label>
              <Select className="mt-1" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                <option>Staff</option>
                <option>Admin</option>
              </Select>
            </div>
            <div>
              <label className="text-xs font-semibold">Contact No. *</label>
              <Input className="mt-1" value={form.contactNo} onChange={(e) => setForm({ ...form, contactNo: e.target.value })} />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit">Create Account</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}

function ExpiryAlertsView({ notify }) {
  const [alerts, setAlerts] = useState([])

  const load = async () => {
    const res = await api('/api/alerts')
    setAlerts(res.alerts || [])
  }

  useEffect(() => {
    load()
  }, [])

  const renewFromAlert = async (a) => {
    await api(`/api/members/${a.userId}/renew`, { method: 'POST' })
    notify(`Renewed ${a.fullName} for 1 month`)
    load()
  }

  return (
    <div className="space-y-4">
      <Card className="p-4 border-l-4 border-l-rose-500 flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold">Expiry &amp; Renewal Radar</h3>
          <p className="text-xs text-zinc-500">Members who are expired or expiring within 7 days</p>
        </div>
        <Button variant="secondary" size="sm" onClick={load}>
          <RefreshCw className="h-3.5 w-3.5" />
          Refresh
        </Button>
      </Card>

      <Card className="overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
              <th className="py-3 px-4">Member</th>
              <th className="py-3 px-4">Contact</th>
              <th className="py-3 px-4">Emergency Contact</th>
              <th className="py-3 px-4">End Date</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">Quick Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
            {alerts.map((a) => (
              <tr key={a.userId} className="hover:bg-zinc-50 dark:hover:bg-zinc-900/60">
                <td className="py-3 px-4 font-medium">{a.fullName}</td>
                <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{a.contactNo}</td>
                <td className="py-3 px-4 text-zinc-500 text-xs">{a.emergencyContact}</td>
                <td className="py-3 px-4 font-mono text-xs">{a.endDate}</td>
                <td className="py-3 px-4">
                  <Badge tone={a.alertStatus === 'Expired' ? 'rose' : 'amber'}>{a.timeline}</Badge>
                </td>
                <td className="py-3 px-4 text-right">
                  <Button size="sm" onClick={() => renewFromAlert(a)}>
                    <RefreshCw className="h-3.5 w-3.5" />
                    Renew +1 Mo
                  </Button>
                </td>
              </tr>
            ))}
            {alerts.length === 0 && (
              <tr>
                <td colSpan={6} className="py-10 text-center text-sm text-zinc-500">All member passes are healthy.</td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
