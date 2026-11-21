import React, { useEffect, useMemo, useState } from 'react'
import { Toaster, toast } from 'sonner'
import { motion } from 'motion/react'
import {
  Activity,
  Bell,
  Dumbbell,
  Eye,
  EyeOff,
  LayoutDashboard,
  LogOut,
  Moon,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Shield,
  Sun,
  Trash2,
  UserPlus,
  Users,
} from 'lucide-react'
import { Badge, Button, Card, Input, Modal, Select, Tabs, TabsContent, TabsList, TabsTrigger, cn } from './components/ui'
import { MemberDrawer } from './components/MemberDrawer'
import { CommandMenu } from './components/CommandMenu'
import { OverviewBento } from './components/OverviewBento'
import { MusclePreviewCard } from './components/MuscleMap'

async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`)
  return data
}

export default function App() {
  const [dark, setDark] = useState(false)
  const [user, setUser] = useState(null)
  const [health, setHealth] = useState({ jdbcConnected: false, isMySql: false, dbLabel: 'Memory DB' })
  const [activeTab, setActiveTab] = useState('overview')

  const [members, setMembers] = useState([])
  const [report, setReport] = useState(null)
  const [drawerMember, setDrawerMember] = useState(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [cmdOpen, setCmdOpen] = useState(false)
  const [memberModalOpen, setMemberModalOpen] = useState(false)
  const [editingMember, setEditingMember] = useState(null)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
  }, [dark])

  useEffect(() => {
    api('/api/health').then(setHealth).catch(() => {})
  }, [])

  const loadWorkspaceData = async () => {
    try {
      const [mRes, rRes] = await Promise.all([api('/api/members'), api('/api/reports')])
      const list = mRes.members || []
      setMembers(list)
      setReport(rRes)
      setDrawerMember((prev) => (prev ? list.find((m) => m.userId === prev.userId) || null : null))
    } catch (e) {
      toast.error(e.message)
    }
  }

  useEffect(() => {
    if (user) loadWorkspaceData()
  }, [user])

  const renewMember = async (m) => {
    try {
      const res = await api(`/api/members/${m.userId}/renew`, { method: 'POST' })
      toast.success(`Renewed ${m.fullName}`, { description: `Active through ${res.endDate}` })
      await loadWorkspaceData()
    } catch (e) {
      toast.error(e.message)
    }
  }

  const deleteMember = async (m) => {
    if (!window.confirm(`Delete member ${m.fullName} (#${m.userId}) and all records?`)) return
    try {
      await api(`/api/members/${m.userId}`, { method: 'DELETE' })
      toast.success(`Deleted ${m.fullName}`)
      setDrawerOpen(false)
      setDrawerMember(null)
      await loadWorkspaceData()
    } catch (e) {
      toast.error(e.message)
    }
  }

  if (!user) {
    return (
      <>
        <Toaster position="bottom-right" richColors theme={dark ? 'dark' : 'light'} />
        <LoginView
          dark={dark}
          onToggleDark={() => setDark(!dark)}
          onSuccess={(authUser) => {
            setUser(authUser)
            setActiveTab('overview')
            toast.success(`Signed in as ${authUser.fullName}`)
          }}
        />
      </>
    )
  }

  const isAdmin = (user.role || '').toLowerCase() === 'admin'
  const navItems = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'directory', label: 'Directory', icon: Users },
    { id: 'workouts', label: 'Workouts', icon: Dumbbell },
    { id: 'radar', label: 'Radar', icon: Bell },
    ...(isAdmin ? [{ id: 'team', label: 'Team', icon: Shield }] : []),
  ]

  return (
    <div className="min-h-screen bg-[#fafaf9] text-zinc-950 dark:bg-zinc-950 dark:text-zinc-50 flex flex-col">
      <Toaster position="bottom-right" richColors theme={dark ? 'dark' : 'light'} />

      {/* Top Glass Command Bar */}
      <header className="sticky top-0 z-30 border-b border-zinc-200/80 bg-white/85 backdrop-blur-md dark:border-zinc-800/80 dark:bg-zinc-950/85">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-5 py-3">
          {/* Brand */}
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-zinc-900 text-white dark:bg-emerald-600">
              <Dumbbell className="h-4 w-4" />
            </div>
            <div className="hidden sm:block">
              <span className="text-sm font-bold tracking-tight">GYMSYNC</span>
              <span className="ml-2 text-xs text-zinc-400">{isAdmin ? 'Admin' : 'Staff'}</span>
            </div>
          </div>

          {/* Motion Spring Pill Nav */}
          <nav className="flex items-center gap-1 rounded-xl border border-zinc-200/80 bg-zinc-100/70 p-1 dark:border-zinc-800 dark:bg-zinc-900/70">
            {navItems.map((item) => {
              const Icon = item.icon
              const active = activeTab === item.id
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={cn(
                    'relative flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors cursor-pointer',
                    active ? 'text-zinc-950 dark:text-zinc-50' : 'text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-200'
                  )}
                >
                  {active && (
                    <motion.span
                      layoutId="activeNavPill"
                      transition={{ type: 'spring', stiffness: 450, damping: 35 }}
                      className="absolute inset-0 rounded-lg bg-white shadow-2xs border border-zinc-200/60 dark:bg-zinc-800 dark:border-zinc-700/60"
                    />
                  )}
                  <Icon className="relative z-10 h-3.5 w-3.5" />
                  <span className="relative z-10">{item.label}</span>
                </button>
              )
            })}
          </nav>

          {/* Right Controls: Ctrl+K Trigger, Theme, User */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCmdOpen(true)}
              className="hidden md:flex items-center gap-2 rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-1.5 text-xs text-zinc-500 hover:bg-zinc-100 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 cursor-pointer"
            >
              <Search className="h-3.5 w-3.5" />
              <span>Quick lookup...</span>
              <kbd className="ml-2 rounded border border-zinc-200 bg-white px-1.5 py-0.5 font-mono text-[10px] dark:border-zinc-700 dark:bg-zinc-950">
                Ctrl K
              </kbd>
            </button>

            <Badge tone={health.jdbcConnected ? 'emerald' : 'neutral'} className="hidden lg:inline-flex">
              <Activity className="h-3 w-3" />
              {health.dbLabel}
            </Badge>

            <Button variant="ghost" size="sm" onClick={() => setDark(!dark)} title="Toggle theme">
              {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </Button>

            <Button variant="secondary" size="sm" onClick={() => setUser(null)}>
              <LogOut className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Sign Out</span>
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content Container */}
      <main className="mx-auto w-full max-w-7xl flex-1 px-5 py-6">
        {activeTab === 'overview' && (
          <OverviewBento
            members={members}
            report={report}
            onSelectMember={(m) => {
              setDrawerMember(m)
              setDrawerOpen(true)
            }}
            onRenewMember={renewMember}
            onNewMember={() => {
              setEditingMember(null)
              setMemberModalOpen(true)
            }}
            onNavigate={setActiveTab}
          />
        )}

        {activeTab === 'directory' && (
          <DirectoryView
            members={members}
            onSelectMember={(m) => {
              setDrawerMember(m)
              setDrawerOpen(true)
            }}
            onNewMember={() => {
              setEditingMember(null)
              setMemberModalOpen(true)
            }}
          />
        )}

        {activeTab === 'workouts' && <WorkoutsHubView isAdmin={isAdmin} members={members} />}
        {activeTab === 'radar' && <ExpiryAlertsView onRenewMember={renewMember} members={members} />}
        {activeTab === 'team' && isAdmin && <StaffAccountsView currentUser={user} />}
      </main>

      {/* Vaul Slide-Over Drawer for Member Pass Inspection */}
      <MemberDrawer
        member={drawerMember}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        isAdmin={isAdmin}
        onRenew={renewMember}
        onEdit={(m) => {
          setEditingMember(m)
          setMemberModalOpen(true)
        }}
        onDelete={deleteMember}
      />

      {/* cmdk Ctrl+K Command Palette */}
      <CommandMenu
        open={cmdOpen}
        onOpenChange={setCmdOpen}
        members={members}
        isAdmin={isAdmin}
        dark={dark}
        onToggleDark={() => setDark(!dark)}
        onNavigate={setActiveTab}
        onSelectMember={(m) => {
          setDrawerMember(m)
          setDrawerOpen(true)
        }}
        onNewMember={() => {
          setEditingMember(null)
          setMemberModalOpen(true)
        }}
      />

      <MemberFormModal
        open={memberModalOpen}
        member={editingMember}
        onClose={() => setMemberModalOpen(false)}
        onSaved={() => {
          setMemberModalOpen(false)
          loadWorkspaceData()
        }}
      />
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
  const [loading, setLoading] = useState(false)

  const submitLogin = async (u, p) => {
    setLoading(true)
    try {
      const data = await api('/api/login', {
        method: 'POST',
        body: JSON.stringify({ username: u, password: p }),
      })
      onSuccess(data.user)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-white dark:bg-zinc-950">
      <div className="hidden lg:flex flex-col justify-between bg-zinc-900 p-12 text-white border-r border-zinc-800">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-600 text-white">
            <Dumbbell className="h-5 w-5" />
          </div>
          <span className="text-lg font-bold tracking-tight">GYMSYNC</span>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, ease: 'easeOut' }}
          className="max-w-md space-y-5"
        >
          <Badge tone="emerald" className="bg-emerald-950 text-emerald-400 border-emerald-800">
            Front Desk Operations
          </Badge>
          <h1 className="text-4xl font-bold leading-tight">
            Gym Membership &amp; Workout Operations
          </h1>
          <p className="text-sm text-zinc-400 leading-relaxed max-w-[42ch]">
            Verify passes in milliseconds, automate 1-month renewals, track expiring athletes, and coach workout plans from one fast operator console.
          </p>
        </motion.div>

        <p className="text-xs text-zinc-500">DIT 2-3 Group 3 • Java + MySQL + React</p>
      </div>

      <div className="flex flex-col justify-between p-8 sm:p-12">
        <div className="flex justify-end">
          <Button variant="secondary" size="sm" onClick={onToggleDark}>
            {dark ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
            {dark ? 'Light' : 'Dark'}
          </Button>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, ease: 'easeOut' }}
          className="mx-auto w-full max-w-sm space-y-6"
        >
          <div>
            <h2 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">Sign in</h2>
            <p className="mt-1 text-sm text-zinc-500">Enter your Admin or Front Desk Staff credentials.</p>
          </div>

          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              submitLogin(username.trim(), password)
            }}
          >
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">Username</label>
              <Input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="admin or staff" autoFocus />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">Password</label>
              <div className="relative">
                <Input
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
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
            <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">
              Quick Demo Operator Sign-In
            </p>
            <div className="grid grid-cols-2 gap-2.5">
              <Button variant="secondary" onClick={() => submitLogin('admin', 'admin123')}>
                <Shield className="h-3.5 w-3.5 text-zinc-500" />
                Admin
              </Button>
              <Button variant="secondary" onClick={() => submitLogin('staff', 'staff123')}>
                <Users className="h-3.5 w-3.5 text-emerald-600" />
                Staff
              </Button>
            </div>
          </div>
        </motion.div>

        <div />
      </div>
    </div>
  )
}

/* ============================================================================
   2. FULL-WIDTH DIRECTORY VIEW (Opens Vaul Drawer on Row Click)
   ============================================================================ */
function DirectoryView({ members, onSelectMember, onNewMember }) {
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all')

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

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Member Directory</h1>
          <p className="text-xs text-zinc-500">Click any athlete row to open their digital pass drawer</p>
        </div>
        <Button onClick={onNewMember}>
          <UserPlus className="h-4 w-4" />
          New Member
        </Button>
      </div>

      <Card className="p-3 flex flex-wrap items-center justify-between gap-3">
        <div className="relative w-80 max-w-full">
          <Search className="h-4 w-4 text-zinc-400 absolute left-3 top-2.5" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Filter by name, @username, phone..."
            className="pl-9"
          />
        </div>

        <Tabs value={filter} onValueChange={setFilter}>
          <TabsList>
            <TabsTrigger value="all">All ({members.length})</TabsTrigger>
            <TabsTrigger value="active">Active</TabsTrigger>
            <TabsTrigger value="expiring">Expiring</TabsTrigger>
            <TabsTrigger value="expired">Expired</TabsTrigger>
          </TabsList>
        </Tabs>
      </Card>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
                <th className="py-3 px-4">ID</th>
                <th className="py-3 px-4">Athlete</th>
                <th className="py-3 px-4">Contact</th>
                <th className="py-3 px-4">Level</th>
                <th className="py-3 px-4">Period</th>
                <th className="py-3 px-4">Balance</th>
                <th className="py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
              {filtered.map((m) => {
                const isAct = m.status === 'Active'
                const isExpSoon = isAct && m.daysRemaining >= 0 && m.daysRemaining <= 7
                return (
                  <tr
                    key={m.userId}
                    onClick={() => onSelectMember(m)}
                    className="cursor-pointer transition-colors hover:bg-zinc-50 dark:hover:bg-zinc-900/60"
                  >
                    <td className="py-3.5 px-4 font-mono text-xs text-zinc-400 tabular-nums">#{m.userId}</td>
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-2.5">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-zinc-100 text-xs font-bold text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200">
                          {m.initials}
                        </span>
                        <div>
                          <div className="font-medium">{m.fullName}</div>
                          <div className="text-xs text-zinc-500">@{m.username}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-zinc-600 dark:text-zinc-300 tabular-nums">{m.contactNo}</td>
                    <td className="py-3.5 px-4 text-zinc-600 dark:text-zinc-300">{m.fitnessLevel}</td>
                    <td className="py-3.5 px-4 font-mono text-xs text-zinc-500 tabular-nums">{m.startDate} → {m.endDate}</td>
                    <td className="py-3.5 px-4 text-xs font-medium tabular-nums">{m.balanceLabel}</td>
                    <td className="py-3.5 px-4">
                      <Badge tone={isAct ? (isExpSoon ? 'amber' : 'emerald') : 'rose'}>
                        {isAct ? (isExpSoon ? 'Expiring' : 'Active') : 'Expired'}
                      </Badge>
                    </td>
                  </tr>
                )
              })}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-sm text-zinc-500">
                    No members match your filter.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}

function MemberFormModal({ open, member, onClose, onSaved }) {
  const isNew = !member
  const [form, setForm] = useState({})

  useEffect(() => {
    setForm(
      member
        ? { ...member, password: '' }
        : {
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
          }
    )
  }, [member, open])

  const submit = async (e) => {
    e.preventDefault()
    try {
      if (isNew) {
        await api('/api/members', { method: 'POST', body: JSON.stringify(form) })
        toast.success(`Registered ${form.firstName} ${form.lastName} with 1-month pass`)
      } else {
        await api(`/api/members/${member.userId}`, { method: 'PUT', body: JSON.stringify(form) })
        toast.success(`Updated profile for ${form.firstName} ${form.lastName}`)
      }
      onSaved()
    } catch (err) {
      toast.error(err.message)
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
            <label className="text-xs font-semibold">Username *</label>
            <Input className="mt-1" value={form.username || ''} onChange={(e) => setForm({ ...form, username: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">{isNew ? 'Password *' : 'New Password (optional)'}</label>
            <Input type="password" className="mt-1" value={form.password || ''} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">First Name *</label>
            <Input className="mt-1" value={form.firstName || ''} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">Last Name *</label>
            <Input className="mt-1" value={form.lastName || ''} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">Contact No. *</label>
            <Input className="mt-1" value={form.contactNo || ''} onChange={(e) => setForm({ ...form, contactNo: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">Fitness Level *</label>
            <Select className="mt-1" value={form.fitnessLevel || 'Beginner'} onChange={(e) => setForm({ ...form, fitnessLevel: e.target.value })}>
              <option>Beginner</option>
              <option>Intermediate</option>
              <option>Advanced</option>
            </Select>
          </div>
          <div>
            <label className="text-xs font-semibold">Emergency Contact Name *</label>
            <Input className="mt-1" value={form.emergencyContactName || ''} onChange={(e) => setForm({ ...form, emergencyContactName: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">Emergency Contact No. *</label>
            <Input className="mt-1" value={form.emergencyContactNo || ''} onChange={(e) => setForm({ ...form, emergencyContactNo: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-semibold">Gender</label>
            <Select className="mt-1" value={form.gender || 'Male'} onChange={(e) => setForm({ ...form, gender: e.target.value })}>
              <option>Male</option>
              <option>Female</option>
              <option>Other</option>
              <option>Prefer not to say</option>
            </Select>
          </div>
          <div>
            <label className="text-xs font-semibold">Birthdate (YYYY-MM-DD)</label>
            <Input className="mt-1" placeholder="2002-05-15" value={form.birthdate || ''} onChange={(e) => setForm({ ...form, birthdate: e.target.value })} />
          </div>
        </div>
        <div>
          <label className="text-xs font-semibold">Address (optional)</label>
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
   3. WORKOUTS HUB (Coach + Exercise Library under Radix Tabs)
   ============================================================================ */
function WorkoutsHubView({ isAdmin, members }) {
  return (
    <Tabs defaultValue="coach" className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Workouts &amp; Coaching</h1>
          <p className="text-xs text-zinc-500">Build member routines and manage gym floor exercises</p>
        </div>
        <TabsList>
          <TabsTrigger value="coach">Workout Coach</TabsTrigger>
          {isAdmin && <TabsTrigger value="library">Exercise Library</TabsTrigger>}
        </TabsList>
      </div>

      <TabsContent value="coach">
        <WorkoutCoachView members={members} />
      </TabsContent>
      {isAdmin && (
        <TabsContent value="library">
          <ExerciseLibraryView />
        </TabsContent>
      )}
    </Tabs>
  )
}

function WorkoutCoachView({ members }) {
  const [memberId, setMemberId] = useState(members[0]?.userId ? String(members[0].userId) : '')
  const [plans, setPlans] = useState([])
  const [planId, setPlanId] = useState('')
  const [exercises, setExercises] = useState([])
  const [selectedEx, setSelectedEx] = useState(null)

  const [goal, setGoal] = useState('General Fitness')
  const [level, setLevel] = useState('Beginner')
  const [days, setDays] = useState('3')
  const [zone, setZone] = useState('Full Gym')

  useEffect(() => {
    api('/api/exercises').then((res) => setExercises(res.exercises || []))
  }, [])

  useEffect(() => {
    if (!memberId && members.length) setMemberId(String(members[0].userId))
  }, [members])

  const loadPlans = async (uid) => {
    if (!uid) return
    const res = await api(`/api/workout-plans?userId=${uid}`)
    const list = res.plans || []
    setPlans(list)
    setPlanId(list[0] ? String(list[0].workoutPlanId) : '')
    setSelectedEx(null)
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
      toast.success('Workout plan generated', { description: res.plan.goal })
      await loadPlans(memberId)
      setPlanId(String(res.plan.workoutPlanId))
    } catch (e) {
      toast.error(e.message)
    }
  }

  const deletePlan = async () => {
    if (!activePlan) return
    await api(`/api/workout-plans/${activePlan.workoutPlanId}`, { method: 'DELETE' })
    toast.success(`Deleted plan #${activePlan.workoutPlanId}`)
    await loadPlans(memberId)
  }

  return (
    <div className="space-y-5">
      <Card className="p-4 grid grid-cols-2 sm:grid-cols-6 gap-3 items-end">
        <div className="col-span-2">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Member</label>
          <Select className="mt-1" value={memberId} onChange={(e) => setMemberId(e.target.value)}>
            {members.map((m) => (
              <option key={m.userId} value={String(m.userId)}>
                #{m.userId} — {m.fullName}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Goal</label>
          <Select className="mt-1" value={goal} onChange={(e) => setGoal(e.target.value)}>
            {['Weight Loss', 'Muscle Gain', 'General Fitness', 'Strength Foundation'].map((g) => (
              <option key={g} value={g}>{g}</option>
            ))}
          </Select>
        </div>
        <div>
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Level</label>
          <Select className="mt-1" value={level} onChange={(e) => setLevel(e.target.value)}>
            {['Beginner', 'Intermediate', 'Advanced'].map((l) => (
              <option key={l} value={l}>{l}</option>
            ))}
          </Select>
        </div>
        <div>
          <label className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Zone</label>
          <Select className="mt-1" value={zone} onChange={(e) => setZone(e.target.value)}>
            {['Full Gym', 'Gym Machines Only', 'Dumbbells', 'Bodyweight'].map((z) => (
              <option key={z} value={z}>{z}</option>
            ))}
          </Select>
        </div>
        <Button onClick={generate} className="w-full">
          <Plus className="h-4 w-4" />
          Generate
        </Button>
      </Card>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <div className="xl:col-span-8 space-y-3">
          <Card className="p-3 flex items-center justify-between gap-3">
            <div className="flex-1">
              <Select value={planId || 'none'} onChange={(e) => setPlanId(e.target.value)}>
                {plans.length === 0 && <option value="none">No saved plans yet</option>}
                {plans.map((p) => (
                  <option key={p.workoutPlanId} value={String(p.workoutPlanId)}>
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
                      <td className="py-3 px-4 font-mono text-xs tabular-nums">{item.sets} × {item.reps}</td>
                    </tr>
                  )
                })}
                {(!activePlan || activePlan.exercises.length === 0) && (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm text-zinc-500">
                      Generate a workout plan above to view exercises.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </Card>
        </div>

        <div className="xl:col-span-4">
          <MusclePreviewCard
            exercise={
              selectedEx ||
              (activePlan?.exercises?.[0] ? exercises.find((e) => e.name === activePlan.exercises[0].exerciseName) : null)
            }
          />
        </div>
      </div>
    </div>
  )
}

function ExerciseLibraryView() {
  const [exercises, setExercises] = useState([])
  const [showMachines, setShowMachines] = useState(true)
  const [showFree, setShowFree] = useState(true)
  const [selectedId, setSelectedId] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const load = async () => {
    const data = await api('/api/exercises')
    setExercises(data.exercises || [])
    if (!selectedId && data.exercises?.length) setSelectedId(data.exercises[0].exerciseId)
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
      toast.success(`Deleted ${ex.name}`)
      load()
    } catch (e) {
      toast.error(e.message)
    }
  }

  return (
    <div className="space-y-4">
      <Card className="p-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
            <input type="checkbox" checked={showMachines} onChange={(e) => setShowMachines(e.target.checked)} className="accent-emerald-600" />
            Gym Machine Workouts
          </label>
          <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
            <input type="checkbox" checked={showFree} onChange={(e) => setShowFree(e.target.checked)} className="accent-emerald-600" />
            Free-Weight / Bodyweight
          </label>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" disabled={!selected} onClick={() => { setEditing(selected); setModalOpen(true) }}>
            <Pencil className="h-3.5 w-3.5" />
            Edit
          </Button>
          <Button variant="danger" size="sm" disabled={!selected} onClick={() => selected && remove(selected)}>
            <Trash2 className="h-3.5 w-3.5" />
            Delete
          </Button>
          <Button size="sm" onClick={() => { setEditing(null); setModalOpen(true) }}>
            <Plus className="h-3.5 w-3.5" />
            Add Exercise
          </Button>
        </div>
      </Card>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <Card className="xl:col-span-8 overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
                <th className="py-3 px-4">ID</th>
                <th className="py-3 px-4">Workout</th>
                <th className="py-3 px-4">Muscle</th>
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
                    <td className="py-3 px-4 font-mono text-xs text-zinc-400">#{ex.exerciseId}</td>
                    <td className="py-3 px-4 font-medium">{ex.name}</td>
                    <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{ex.muscleGroup}</td>
                    <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300">{ex.difficulty}</td>
                    <td className="py-3 px-4">
                      <Badge tone={isMach ? 'blue' : 'emerald'}>{ex.equipment.replace('Machine: ', '')}</Badge>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </Card>
        <div className="xl:col-span-4">
          <MusclePreviewCard exercise={selected} />
        </div>
      </div>

      <ExerciseModal open={modalOpen} exercise={editing} onClose={() => setModalOpen(false)} onSaved={() => { setModalOpen(false); load() }} />
    </div>
  )
}

function ExerciseModal({ open, exercise, onClose, onSaved }) {
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
        toast.success(`Updated ${form.name}`)
      } else {
        await api('/api/exercises', { method: 'POST', body: JSON.stringify(form) })
        toast.success(`Added ${form.name}`)
      }
      onSaved()
    } catch (err) {
      toast.error(err.message)
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
                <option key={g} value={g}>{g}</option>
              ))}
            </Select>
          </div>
          <div>
            <label className="text-xs font-semibold">Difficulty</label>
            <Select className="mt-1" value={form.difficulty || 'Beginner'} onChange={(e) => setForm({ ...form, difficulty: e.target.value })}>
              {['Beginner', 'Intermediate', 'Advanced'].map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </Select>
          </div>
        </div>
        <div>
          <label className="text-xs font-semibold">Equipment</label>
          <Input className="mt-1" value={form.equipment || ''} onChange={(e) => setForm({ ...form, equipment: e.target.value })} />
        </div>
        <div>
          <label className="text-xs font-semibold">Execution Cues *</label>
          <textarea
            rows={3}
            className="mt-1 w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-900/60"
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
   4. RADAR & TEAM VIEWS
   ============================================================================ */
function ExpiryAlertsView({ members, onRenewMember }) {
  const alerts = members.filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7))

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">Expiry &amp; Renewal Radar</h1>
        <p className="text-xs text-zinc-500">Athletes whose 1-month pass is expired or expiring within 7 days</p>
      </div>

      <Card className="overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-zinc-200 bg-zinc-50/70 dark:border-zinc-800 dark:bg-zinc-900/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-500">
              <th className="py-3 px-4">Member</th>
              <th className="py-3 px-4">Contact</th>
              <th className="py-3 px-4">Emergency Contact</th>
              <th className="py-3 px-4">End Date</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800 text-sm">
            {alerts.map((a) => (
              <tr key={a.userId} className="hover:bg-zinc-50 dark:hover:bg-zinc-900/60">
                <td className="py-3.5 px-4 font-medium">{a.fullName}</td>
                <td className="py-3.5 px-4 text-zinc-600 dark:text-zinc-300 tabular-nums">{a.contactNo}</td>
                <td className="py-3.5 px-4 text-zinc-500 text-xs">{a.emergencyContactName} ({a.emergencyContactNo})</td>
                <td className="py-3.5 px-4 font-mono text-xs tabular-nums">{a.endDate}</td>
                <td className="py-3.5 px-4">
                  <Badge tone={a.status !== 'Active' ? 'rose' : 'amber'}>{a.balanceLabel}</Badge>
                </td>
                <td className="py-3.5 px-4 text-right">
                  <Button size="sm" onClick={() => onRenewMember(a)}>
                    <RefreshCw className="h-3.5 w-3.5" />
                    Renew +1 Mo
                  </Button>
                </td>
              </tr>
            ))}
            {alerts.length === 0 && (
              <tr>
                <td colSpan={6} className="py-12 text-center text-sm text-zinc-500">All member passes are active and healthy.</td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </div>
  )
}

function StaffAccountsView({ currentUser }) {
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
      toast.success(`Created account @${form.username}`)
      setOpen(false)
      setForm({ username: '', password: '', role: 'Staff', firstName: '', lastName: '', contactNo: '09' })
      load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const remove = async (u) => {
    if (u.userId === currentUser.userId) return toast.error('Cannot delete your own active Admin account.')
    if (!window.confirm(`Delete @${u.username}?`)) return
    await api(`/api/staff/${u.userId}`, { method: 'DELETE' })
    toast.success(`Deleted @${u.username}`)
    load()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Team &amp; Accounts</h1>
          <p className="text-xs text-zinc-500">Manage Admin, Front Desk Staff, and registered user records</p>
        </div>
        <Button onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4" />
          Add Operator
        </Button>
      </div>

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
                <td className="py-3 px-4 font-mono text-xs text-zinc-400">#{u.userId}</td>
                <td className="py-3 px-4 font-medium">{u.fullName}</td>
                <td className="py-3 px-4 text-zinc-500">@{u.username}</td>
                <td className="py-3 px-4">
                  <Badge tone={u.role === 'Admin' ? 'blue' : u.role === 'Staff' ? 'emerald' : 'neutral'}>{u.role}</Badge>
                </td>
                <td className="py-3 px-4 text-zinc-600 dark:text-zinc-300 tabular-nums">{u.contactNo}</td>
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
                <option value="Staff">Staff</option>
                <option value="Admin">Admin</option>
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
