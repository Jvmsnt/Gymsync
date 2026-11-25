import React, { useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { AnimatePresence, motion } from 'motion/react'
import {
  Activity,
  Bell,
  ChevronsUpDown,
  Download,
  Dumbbell,
  Eye,
  EyeOff,
  LayoutDashboard,
  LogOut,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Shield,
  ShieldCheck,
  Trash2,
  UserPlus,
  Users,
} from 'lucide-react'
import { Toaster } from '@/components/ui/sonner'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { DotPattern } from '@/components/ui/dot-pattern'
import { Spotlight } from '@/components/ui/spotlight'
import { BorderBeam } from '@/components/ui/border-beam'
import { BaseActionMenu, StudioSwitch } from '@/components/BaseControls'
import { MemberDrawer } from '@/components/MemberDrawer'
import { CommandMenu } from '@/components/CommandMenu'
import { OverviewBento } from '@/components/OverviewBento'
import { MusclePreviewCard } from '@/components/MuscleMap'
import { cn } from '@/lib/utils'

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
      await loadWorkspaceData()
      toast.success(`Renewed ${m.fullName}`, {
        description: `1-month pass active through ${res.endDate}`,
        action: {
          label: 'Inspect Pass',
          onClick: () => {
            setDrawerMember(m)
            setDrawerOpen(true)
          },
        },
      })
    } catch (e) {
      toast.error(e.message)
    }
  }

  const deleteMember = async (m) => {
    if (!window.confirm(`Delete member ${m.fullName} (#${m.userId}) and all associated records?`)) return
    try {
      await api(`/api/members/${m.userId}`, { method: 'DELETE' })
      toast.success(`Removed ${m.fullName} from roster`)
      setDrawerOpen(false)
      setDrawerMember(null)
      await loadWorkspaceData()
    } catch (e) {
      toast.error(e.message)
    }
  }

  if (!user) {
    return (
      <TooltipProvider>
        <Toaster position="bottom-right" theme={dark ? 'dark' : 'light'} />
        <LoginView
          dark={dark}
          onToggleDark={() => setDark(!dark)}
          onSuccess={(authUser) => {
            setUser(authUser)
            setActiveTab('overview')
            toast.success(`Signed in as ${authUser.fullName}`)
          }}
        />
      </TooltipProvider>
    )
  }

  const isAdmin = (user.role || '').toLowerCase() === 'admin'
  const alertCount = members.filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7)).length

  const navGroups = [
    {
      label: 'Operations',
      items: [
        { id: 'overview', label: 'Dashboard', icon: LayoutDashboard },
        { id: 'directory', label: 'Directory', icon: Users, badge: members.length },
        { id: 'radar', label: 'Expiry Radar', icon: Bell, badge: alertCount || null, alert: alertCount > 0 },
      ],
    },
    {
      label: 'Coaching',
      items: [{ id: 'workouts', label: 'Workouts Hub', icon: Dumbbell }],
    },
    ...(isAdmin
      ? [
          {
            label: 'Administration',
            items: [{ id: 'team', label: 'Staff & Accounts', icon: Shield }],
          },
        ]
      : []),
  ]

  const flatNav = navGroups.flatMap((g) => g.items)
  const currentNav = flatNav.find((n) => n.id === activeTab) || flatNav[0]

  const operatorInitials = (user.fullName || user.username || 'OP')
    .split(' ')
    .map((s) => s[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  return (
    <TooltipProvider>
      <div className="h-screen w-screen overflow-hidden flex bg-muted text-foreground">
        <Toaster position="bottom-right" theme={dark ? 'dark' : 'light'} />

        {/* Inset Application Sidebar */}
        <aside className="w-60 shrink-0 flex flex-col justify-between p-3 select-none">
          <div className="space-y-5">
            {/* Brand + App Pill */}
            <div className="flex items-center justify-between px-2 pt-1">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground shadow-xs">
                  <Dumbbell className="h-4 w-4" />
                </div>
                <div>
                  <p className="font-display text-sm font-bold leading-none tracking-tight">GymSync</p>
                  <p className="mt-0.5 text-[10px] font-medium text-muted-foreground">Desk OS · DIT 2-3</p>
                </div>
              </div>
              <Badge variant="outline" className="text-[10px] font-mono px-1.5 py-0">
                {isAdmin ? 'ADM' : 'STF'}
              </Badge>
            </div>

            {/* Sidebar Grouped Navigation with Motion Spring Indicator */}
            <div className="space-y-4">
              {navGroups.map((group) => (
                <div key={group.label} className="space-y-1">
                  <p className="px-2.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                    {group.label}
                  </p>
                  <div className="space-y-0.5">
                    {group.items.map((item) => {
                      const Icon = item.icon
                      const active = activeTab === item.id
                      return (
                        <button
                          key={item.id}
                          onClick={() => setActiveTab(item.id)}
                          className={cn(
                            'relative w-full flex items-center justify-between rounded-lg px-2.5 py-2 text-[13px] font-medium transition-colors cursor-pointer',
                            active
                              ? 'text-foreground'
                              : 'text-muted-foreground hover:text-foreground hover:bg-background/50'
                          )}
                        >
                          {active && (
                            <motion.span
                              layoutId="sidebarActiveNav"
                              transition={{ type: 'spring', stiffness: 460, damping: 36 }}
                              className="absolute inset-0 rounded-lg bg-background border shadow-2xs"
                            />
                          )}
                          <span className="relative z-10 flex items-center gap-2.5">
                            <Icon className="h-4 w-4 shrink-0" />
                            <span>{item.label}</span>
                          </span>
                          {item.badge != null && (
                            <span
                              className={cn(
                                'relative z-10 rounded-md px-1.5 py-0.5 font-mono text-[10px] tabular-nums',
                                item.alert
                                  ? 'bg-rose-500/12 text-rose-600 dark:text-rose-400 font-semibold'
                                  : 'bg-muted text-muted-foreground'
                              )}
                            >
                              {item.badge}
                            </span>
                          )}
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Bottom Dock: Theme BaseUI Switch + DB Health + BaseUI Operator Menu */}
          <div className="space-y-2.5 pt-3 border-t">
            <div className="px-2.5 py-1.5 rounded-lg bg-background/60 border">
              <StudioSwitch
                id="sidebar-theme-switch"
                checked={dark}
                onCheckedChange={setDark}
                label="Obsidian Mode"
                description={dark ? 'Low-glare dark canvas' : 'Architectural light'}
              />
            </div>

            <Tooltip>
              <TooltipTrigger asChild>
                <div className="flex items-center justify-between px-2.5 py-1.5 text-[11px] text-muted-foreground cursor-default">
                  <span className="flex items-center gap-1.5">
                    <Activity className="h-3.5 w-3.5 text-emerald-500" />
                    <span>{health.dbLabel}</span>
                  </span>
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                </div>
              </TooltipTrigger>
              <TooltipContent side="right">
                {health.jdbcConnected ? 'Connected via JDBC' : 'Running with instant seeded dataset'}
              </TooltipContent>
            </Tooltip>

            <BaseActionMenu
              trigger={
                <div className="flex items-center justify-between gap-2 rounded-xl border bg-background p-2 hover:bg-accent/60 transition-colors cursor-pointer">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <Avatar className="h-8 w-8 border">
                      <AvatarFallback className="text-[11px] font-semibold">{operatorInitials}</AvatarFallback>
                    </Avatar>
                    <div className="min-w-0">
                      <p className="truncate text-xs font-semibold">{user.fullName}</p>
                      <p className="truncate font-mono text-[10px] text-muted-foreground">@{user.username}</p>
                    </div>
                  </div>
                  <ChevronsUpDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                </div>
              }
              items={[
                {
                  label: 'Command Palette (Ctrl+K)',
                  icon: <Search className="h-3.5 w-3.5" />,
                  onClick: () => setCmdOpen(true),
                },
                {
                  label: 'Register New Member',
                  icon: <UserPlus className="h-3.5 w-3.5" />,
                  onClick: () => {
                    setEditingMember(null)
                    setMemberModalOpen(true)
                  },
                },
                ...(isAdmin
                  ? [
                      {
                        label: 'Export CSV Report',
                        icon: <Download className="h-3.5 w-3.5" />,
                        onClick: () => (window.location.href = '/api/reports/csv'),
                      },
                    ]
                  : []),
                { separator: true },
                {
                  label: 'Sign Out',
                  icon: <LogOut className="h-3.5 w-3.5" />,
                  danger: true,
                  onClick: () => setUser(null),
                },
              ]}
            />
          </div>
        </aside>

        {/* Rounded Inset Main Application Window */}
        <section className="flex-1 my-2 mr-2 rounded-xl border bg-background shadow-xs flex flex-col overflow-hidden">
          {/* Contextual Top Header */}
          <header className="h-13 shrink-0 border-b px-6 flex items-center justify-between gap-4 bg-background/90 backdrop-blur-xs">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <span>GymSync</span>
              <span>/</span>
              <span className="font-medium text-foreground">{currentNav?.label}</span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setCmdOpen(true)}
                className="flex items-center gap-2 rounded-lg border bg-muted/40 px-3 py-1.5 text-xs text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
              >
                <Search className="h-3.5 w-3.5" />
                <span>Jump to member or action...</span>
                <kbd className="ml-3 rounded border bg-background px-1.5 py-0.5 font-mono text-[10px]">
                  Ctrl K
                </kbd>
              </button>

              <Button variant="ghost" size="icon-sm" onClick={loadWorkspaceData} title="Sync roster">
                <RefreshCw className="h-3.5 w-3.5" />
              </Button>
            </div>
          </header>

          {/* Scrollable Workspace Content */}
          <main className="flex-1 overflow-y-auto px-6 py-6">
            <div className="mx-auto max-w-6xl">
              <AnimatePresence mode="wait">
                <motion.div
                  key={activeTab}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  transition={{ duration: 0.16, ease: 'easeOut' }}
                >
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
                </motion.div>
              </AnimatePresence>
            </div>
          </main>
        </section>

        {/* Vaul Slide-Over Drawer for Digital Member Pass */}
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
    </TooltipProvider>
  )
}

/* ============================================================================
   1. ARCHITECTURAL SPLIT LOGIN (Aceternity Spotlight + MagicUI DotPattern & BorderBeam)
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
    <div className="min-h-screen grid lg:grid-cols-12 bg-background text-foreground">
      {/* Left Showcase Canvas */}
      <div className="hidden lg:flex lg:col-span-7 relative overflow-hidden flex-col justify-between bg-zinc-950 p-12 text-zinc-50 border-r border-zinc-800/80">
        <Spotlight className="-top-40 left-0 md:left-40 md:-top-20" fill="#10b981" />
        <DotPattern
          width={22}
          height={22}
          cr={1}
          className="text-zinc-800/80 [mask-image:radial-gradient(600px_circle_at_center,white,transparent)]"
        />

        <div className="relative z-10 flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-zinc-950 shadow-sm">
            <Dumbbell className="h-4 w-4" />
          </div>
          <div>
            <span className="font-display text-base font-bold tracking-tight">GYMSYNC</span>
            <span className="ml-2 font-mono text-[11px] text-zinc-500">v2.0 DESK</span>
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: 'easeOut' }}
          className="relative z-10 max-w-lg space-y-6"
        >
          <div className="space-y-3">
            <span className="inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-900/90 px-3 py-1 text-xs font-medium text-zinc-300">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
              Front Desk &amp; Workout Operations
            </span>
            <h1 className="text-4xl font-bold tracking-tight leading-[1.12] text-white">
              Fast desk check-ins. Zero spreadsheet guesswork.
            </h1>
            <p className="text-sm text-zinc-400 leading-relaxed max-w-[46ch]">
              Verify member eligibility instantaneously, trigger automated 1-month pass cycles, monitor expiring athletes, and coach structured training plans in one operator console.
            </p>
          </div>

          {/* Preview Standout Pass Card */}
          <div className="relative rounded-2xl border border-zinc-800 bg-zinc-900/75 backdrop-blur-md p-5 max-w-md overflow-hidden">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-widest text-zinc-500">Live Desk Verification</p>
                <p className="mt-1 font-semibold text-sm text-white">Carlos Mendoza · Pass #0003</p>
              </div>
              <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/30 bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-medium text-emerald-300">
                <ShieldCheck className="h-3 w-3" />
                Cleared
              </span>
            </div>
            <div className="mt-4 pt-3 border-t border-zinc-800 flex items-center justify-between font-mono text-xs text-zinc-400">
              <span>Balance: 18 days remaining</span>
              <span className="text-emerald-400">+1 Mo Ready</span>
            </div>
            <BorderBeam size={90} duration={7} colorFrom="#10b981" colorTo="#06b6d4" />
          </div>
        </motion.div>

        <div className="relative z-10 flex items-center justify-between text-xs text-zinc-500 font-mono">
          <span>DIT 2-3 Group 3 · Integrative Stack</span>
          <span>Admin &amp; Staff Operators Only</span>
        </div>
      </div>

      {/* Right Operator Sign-In Column */}
      <div className="lg:col-span-5 flex flex-col justify-between p-8 sm:p-12">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 lg:hidden">
            <Dumbbell className="h-4 w-4" />
            <span className="font-display font-bold">GYMSYNC</span>
          </div>
          <div className="ml-auto">
            <StudioSwitch id="login-theme" checked={dark} onCheckedChange={onToggleDark} label={dark ? 'Dark' : 'Light'} />
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          className="mx-auto w-full max-w-sm space-y-6"
        >
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Operator Sign In</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Authenticate with your Admin or Front Desk Staff credentials.
            </p>
          </div>

          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              submitLogin(username.trim(), password)
            }}
          >
            <div className="space-y-1.5">
              <Label htmlFor="login-user">Username</Label>
              <Input
                id="login-user"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="admin or staff"
                autoFocus
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="login-pass">Password</Label>
              <div className="relative">
                <Input
                  id="login-pass"
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  className="pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-2.5 top-2.5 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  {showPass ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" size="lg" className="w-full cursor-pointer" disabled={loading}>
              {loading ? 'Authenticating...' : 'Open Desk Console'}
            </Button>
          </form>

          <Separator />

          <div className="space-y-2.5">
            <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              1-Click Demo Operator Access
            </p>
            <div className="grid grid-cols-2 gap-2.5">
              <Button variant="outline" onClick={() => submitLogin('admin', 'admin123')} className="cursor-pointer">
                <Shield className="h-3.5 w-3.5" />
                Admin Desk
              </Button>
              <Button variant="outline" onClick={() => submitLogin('staff', 'staff123')} className="cursor-pointer">
                <Users className="h-3.5 w-3.5 text-emerald-600" />
                Staff Desk
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
   2. DIRECTORY VIEW (shadcn Table + Tabs + Vaul Drawer on row click)
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
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Member Directory</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Click any member row to slide out their interactive Digital Member Pass
          </p>
        </div>
        <Button size="sm" onClick={onNewMember} className="cursor-pointer">
          <UserPlus className="h-3.5 w-3.5" />
          Register Member
        </Button>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="relative w-80 max-w-full">
          <Search className="h-4 w-4 text-muted-foreground absolute left-3 top-2.5" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Filter by athlete, @handle, phone..."
            className="pl-9"
          />
        </div>

        <Tabs value={filter} onValueChange={setFilter}>
          <TabsList>
            <TabsTrigger value="all">All ({members.length})</TabsTrigger>
            <TabsTrigger value="active">Active</TabsTrigger>
            <TabsTrigger value="expiring">Expiring ≤7d</TabsTrigger>
            <TabsTrigger value="expired">Expired</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <Card className="p-0 overflow-hidden shadow-none">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40 hover:bg-muted/40">
              <TableHead className="w-16">ID</TableHead>
              <TableHead>Athlete</TableHead>
              <TableHead>Contact</TableHead>
              <TableHead>Tier</TableHead>
              <TableHead>Current Cycle</TableHead>
              <TableHead>Balance</TableHead>
              <TableHead className="text-right">Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((m) => {
              const isAct = m.status === 'Active'
              const isExpSoon = isAct && m.daysRemaining >= 0 && m.daysRemaining <= 7
              return (
                <TableRow
                  key={m.userId}
                  onClick={() => onSelectMember(m)}
                  className="cursor-pointer"
                >
                  <TableCell className="font-mono text-xs text-muted-foreground tabular-nums">
                    #{String(m.userId).padStart(3, '0')}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2.5">
                      <Avatar className="h-7 w-7 border">
                        <AvatarFallback className="text-[10px] font-semibold">{m.initials}</AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="font-medium text-sm leading-none">{m.fullName}</p>
                        <p className="font-mono text-[11px] text-muted-foreground mt-0.5">@{m.username}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="font-mono text-xs tabular-nums text-muted-foreground">
                    {m.contactNo}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className="text-[11px] font-normal">
                      {m.fitnessLevel}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground tabular-nums">
                    {m.startDate} → {m.endDate}
                  </TableCell>
                  <TableCell className="text-xs font-medium tabular-nums">{m.balanceLabel}</TableCell>
                  <TableCell className="text-right">
                    <span
                      className={cn(
                        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-medium border',
                        isAct && !isExpSoon && 'border-emerald-500/25 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400',
                        isExpSoon && 'border-amber-500/25 bg-amber-500/10 text-amber-700 dark:text-amber-400',
                        !isAct && 'border-rose-500/25 bg-rose-500/10 text-rose-700 dark:text-rose-400'
                      )}
                    >
                      <span
                        className={cn(
                          'h-1.5 w-1.5 rounded-full',
                          isAct && !isExpSoon ? 'bg-emerald-500' : isExpSoon ? 'bg-amber-500' : 'bg-rose-500'
                        )}
                      />
                      {isAct ? (isExpSoon ? 'Expiring' : 'Active') : 'Expired'}
                    </span>
                  </TableCell>
                </TableRow>
              )
            })}
            {filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="py-12 text-center text-sm text-muted-foreground">
                  No members match your search criteria.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
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
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{isNew ? 'Register New Member' : `Edit Profile — ${member?.fullName}`}</DialogTitle>
          <DialogDescription>
            {isNew ? 'Issues an active 1-month studio pass starting today.' : 'Update athlete dossier fields.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <div className="space-y-1">
              <Label>Username *</Label>
              <Input value={form.username || ''} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>{isNew ? 'Password *' : 'New Password (optional)'}</Label>
              <Input type="password" value={form.password || ''} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>First Name *</Label>
              <Input value={form.firstName || ''} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>Last Name *</Label>
              <Input value={form.lastName || ''} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>Contact No. *</Label>
              <Input value={form.contactNo || ''} onChange={(e) => setForm({ ...form, contactNo: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>Fitness Level *</Label>
              <Select value={form.fitnessLevel || 'Beginner'} onValueChange={(v) => setForm({ ...form, fitnessLevel: v })}>
                <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="Beginner">Beginner</SelectItem>
                  <SelectItem value="Intermediate">Intermediate</SelectItem>
                  <SelectItem value="Advanced">Advanced</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Emergency Contact Name *</Label>
              <Input value={form.emergencyContactName || ''} onChange={(e) => setForm({ ...form, emergencyContactName: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>Emergency Contact No. *</Label>
              <Input value={form.emergencyContactNo || ''} onChange={(e) => setForm({ ...form, emergencyContactNo: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>Gender</Label>
              <Select value={form.gender || 'Male'} onValueChange={(v) => setForm({ ...form, gender: v })}>
                <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="Male">Male</SelectItem>
                  <SelectItem value="Female">Female</SelectItem>
                  <SelectItem value="Other">Other</SelectItem>
                  <SelectItem value="Prefer not to say">Prefer not to say</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Birthdate (YYYY-MM-DD)</Label>
              <Input placeholder="2002-05-15" value={form.birthdate || ''} onChange={(e) => setForm({ ...form, birthdate: e.target.value })} />
            </div>
          </div>

          <div className="space-y-1">
            <Label>Address (optional)</Label>
            <Input value={form.address || ''} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit">{isNew ? 'Activate Member Pass' : 'Save Changes'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/* ============================================================================
   3. WORKOUTS HUB
   ============================================================================ */
function WorkoutsHubView({ isAdmin, members }) {
  return (
    <Tabs defaultValue="coach" className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Workouts &amp; Coaching</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Generate rule-based athlete training splits and inspect target muscle stations
          </p>
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
  const [level, setLevel] = useState(members[0]?.fitnessLevel || 'Beginner')
  const [daysPerWeek, setDaysPerWeek] = useState('3')
  const [zone, setZone] = useState('Full Gym')
  const [generating, setGenerating] = useState(false)
  const [customOpen, setCustomOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)

  useEffect(() => {
    api('/api/exercises')
      .then((res) => setExercises(res.exercises || []))
      .catch((e) => toast.error(e.message))
  }, [])

  useEffect(() => {
    if (!memberId && members.length) {
      setMemberId(String(members[0].userId))
      if (members[0].fitnessLevel) setLevel(members[0].fitnessLevel)
    }
  }, [members])

  const selectedMember = members.find((m) => String(m.userId) === String(memberId)) || null

  const loadPlans = async (uid) => {
    if (!uid) return
    try {
      const res = await api(`/api/workout-plans?userId=${uid}`)
      const list = res.plans || []
      setPlans(list)
      setPlanId(list[0] ? String(list[0].workoutPlanId) : '')
      setSelectedEx(null)
    } catch (e) {
      toast.error(e.message)
    }
  }

  useEffect(() => {
    loadPlans(memberId)
  }, [memberId])

  const activePlan = plans.find((p) => String(p.workoutPlanId) === String(planId)) || plans[0] || null

  const generate = async () => {
    if (!memberId) return toast.error('Select an athlete first.')
    setGenerating(true)
    try {
      const res = await api('/api/workout-plans/generate', {
        method: 'POST',
        body: JSON.stringify({
          userId: Number(memberId),
          goal,
          fitnessLevel: level,
          daysPerWeek: Number(daysPerWeek),
          equipmentPref: zone,
          includeMachines: zone === 'Full Gym' || zone === 'Gym Machines Only',
        }),
      })
      toast.success('Generated training split', { description: res.plan.goal })
      await loadPlans(memberId)
      setPlanId(String(res.plan.workoutPlanId))
    } catch (e) {
      toast.error(e.message)
    } finally {
      setGenerating(false)
    }
  }

  const deletePlan = async () => {
    if (!activePlan) return
    try {
      await api(`/api/workout-plans/${activePlan.workoutPlanId}`, { method: 'DELETE' })
      toast.success(`Removed Plan #${activePlan.workoutPlanId}`)
      setDeleteDialogOpen(false)
      await loadPlans(memberId)
    } catch (e) {
      toast.error(e.message)
    }
  }

  return (
    <div className="space-y-5">
      <Card className="py-4 px-5 shadow-none">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
          <div className="md:col-span-3 space-y-1">
            <Label className="text-[11px] uppercase tracking-wider text-muted-foreground">Athlete</Label>
            <Select
              value={memberId}
              onValueChange={(uid) => {
                setMemberId(uid)
                const next = members.find((m) => String(m.userId) === String(uid))
                if (next?.fitnessLevel) setLevel(next.fitnessLevel)
              }}
            >
              <SelectTrigger className="w-full"><SelectValue placeholder="Choose member" /></SelectTrigger>
              <SelectContent>
                {members.map((m) => (
                  <SelectItem key={m.userId} value={String(m.userId)}>
                    #{m.userId} — {m.fullName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="md:col-span-2 space-y-1">
            <Label className="text-[11px] uppercase tracking-wider text-muted-foreground">Goal</Label>
            <Select value={goal} onValueChange={setGoal}>
              <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['Weight Loss', 'Muscle Gain', 'General Fitness', 'Strength Foundation'].map((g) => (
                  <SelectItem key={g} value={g}>{g}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="md:col-span-2 space-y-1">
            <Label className="text-[11px] uppercase tracking-wider text-muted-foreground">Level</Label>
            <Select value={level} onValueChange={setLevel}>
              <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['Beginner', 'Intermediate', 'Advanced'].map((l) => (
                  <SelectItem key={l} value={l}>{l}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="md:col-span-1 space-y-1">
            <Label className="text-[11px] uppercase tracking-wider text-muted-foreground">Split</Label>
            <Select value={daysPerWeek} onValueChange={setDaysPerWeek}>
              <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['2', '3', '4', '5'].map((d) => (
                  <SelectItem key={d} value={d}>{d}d/wk</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="md:col-span-2 space-y-1">
            <Label className="text-[11px] uppercase tracking-wider text-muted-foreground">Equipment Zone</Label>
            <Select value={zone} onValueChange={setZone}>
              <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['Full Gym', 'Gym Machines Only', 'Dumbbells', 'Bodyweight'].map((z) => (
                  <SelectItem key={z} value={z}>{z}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="md:col-span-2 flex gap-2">
            <Button onClick={generate} disabled={generating || !memberId} className="flex-1 cursor-pointer active:scale-[0.97]">
              <Plus className="h-4 w-4" />
              {generating ? 'Saving...' : 'Generate'}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={!memberId || exercises.length === 0}
              onClick={() => setCustomOpen(true)}
              className="cursor-pointer active:scale-[0.97]"
            >
              Custom
            </Button>
          </div>
        </div>
      </Card>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <div className="xl:col-span-8 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex-1 min-w-[240px]">
              <Select value={planId || 'none'} onValueChange={(v) => { setPlanId(v); setSelectedEx(null) }}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="No saved plans" />
                </SelectTrigger>
                <SelectContent>
                  {plans.length === 0 && <SelectItem value="none">No saved plans yet</SelectItem>}
                  {plans.map((p) => (
                    <SelectItem key={p.workoutPlanId} value={String(p.workoutPlanId)}>
                      Plan #{p.workoutPlanId} [{p.planType}] — {p.goal}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {activePlan && (
              <div className="flex items-center gap-2">
                <Badge variant={activePlan.planType === 'custom' ? 'default' : 'secondary'} className="capitalize">
                  {activePlan.planType}
                </Badge>
                {activePlan.dateCreated && (
                  <span className="font-mono text-xs text-muted-foreground tabular-nums">
                    {activePlan.dateCreated}
                  </span>
                )}
              </div>
            )}
            <Button
              variant="outline"
              size="sm"
              disabled={!activePlan}
              onClick={() => setDeleteDialogOpen(true)}
              className="text-rose-600 cursor-pointer active:scale-[0.97]"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete Plan
            </Button>
          </div>

          <Card className="p-0 overflow-hidden shadow-none">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/40 hover:bg-muted/40">
                  <TableHead>Day</TableHead>
                  <TableHead>Exercise</TableHead>
                  <TableHead>Muscle</TableHead>
                  <TableHead>Station</TableHead>
                  <TableHead className="text-right">Sets × Reps</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(activePlan?.exercises || []).map((item, idx) => {
                  const fullEx = exercises.find((e) => e.name.toLowerCase() === item.exerciseName.toLowerCase()) || null
                  const isRowSelected = selectedEx && fullEx && selectedEx.exerciseId === fullEx.exerciseId
                  return (
                    <TableRow
                      key={idx}
                      onClick={() => setSelectedEx(fullEx)}
                      className={cn('cursor-pointer', isRowSelected && 'bg-muted/70')}
                    >
                      <TableCell className="font-medium">{item.dayOfWeek}</TableCell>
                      <TableCell className="font-medium">{item.exerciseName}</TableCell>
                      <TableCell className="text-muted-foreground">{item.muscleGroup}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{(item.equipment || '').replace('Machine: ', '')}</Badge>
                      </TableCell>
                      <TableCell className="text-right font-mono text-xs tabular-nums">
                        {item.sets} × {item.reps}
                      </TableCell>
                    </TableRow>
                  )
                })}
                {(!activePlan || activePlan.exercises.length === 0) && (
                  <TableRow>
                    <TableCell colSpan={5} className="py-10 text-center text-sm text-muted-foreground">
                      Generate a workout plan or build a custom split above to view athlete exercises.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
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

      <CustomPlanModal
        open={customOpen}
        member={selectedMember}
        exercises={exercises}
        onClose={() => setCustomOpen(false)}
        onSaved={async (created) => {
          setCustomOpen(false)
          await loadPlans(memberId)
          if (created?.workoutPlanId) setPlanId(String(created.workoutPlanId))
        }}
      />

      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Delete Workout Plan</DialogTitle>
            <DialogDescription>
              Remove Plan #{activePlan?.workoutPlanId} ({activePlan?.goal}) from {selectedMember?.fullName || 'this athlete'}? This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setDeleteDialogOpen(false)}>
              Cancel
            </Button>
            <Button type="button" variant="destructive" onClick={deletePlan} className="cursor-pointer active:scale-[0.97]">
              Delete Plan
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function CustomPlanModal({ open, member, exercises, onClose, onSaved }) {
  const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
  const [goal, setGoal] = useState('')
  const [dayOfWeek, setDayOfWeek] = useState('Monday')
  const [exerciseId, setExerciseId] = useState('')
  const [sets, setSets] = useState('3')
  const [reps, setReps] = useState('10-12')
  const [items, setItems] = useState([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) return
    setGoal('')
    setDayOfWeek('Monday')
    setExerciseId(exercises[0] ? String(exercises[0].exerciseId) : '')
    setSets('3')
    setReps('10-12')
    setItems([])
  }, [open, exercises])

  const addDraftExercise = () => {
    const matched = exercises.find((e) => String(e.exerciseId) === String(exerciseId))
    if (!matched) return toast.error('Select an exercise to add.')
    const parsedSets = Number.parseInt(sets, 10)
    if (!Number.isInteger(parsedSets) || parsedSets < 1 || parsedSets > 20) {
      return toast.error('Sets must be a number between 1 and 20.')
    }
    const cleanReps = reps.trim()
    if (!cleanReps || cleanReps.length > 25) {
      return toast.error('Enter target reps (1–25 characters, e.g., 10-12 or 30 sec).')
    }
    setItems((prev) => [
      ...prev,
      {
        key: `${Date.now()}-${Math.random()}`,
        exerciseId: matched.exerciseId,
        exerciseName: matched.name,
        muscleGroup: matched.muscleGroup,
        dayOfWeek,
        sets: parsedSets,
        reps: cleanReps,
      },
    ])
  }

  const removeDraftExercise = (key) => {
    setItems((prev) => prev.filter((item) => item.key !== key))
  }

  const submit = async (e) => {
    e.preventDefault()
    if (!member?.userId) return toast.error('No athlete selected.')
    const cleanGoal = goal.trim()
    if (!cleanGoal) return toast.error('Enter a routine title.')
    if (items.length === 0) return toast.error('Add at least one exercise to the routine.')

    setSaving(true)
    try {
      const res = await api('/api/workout-plans', {
        method: 'POST',
        body: JSON.stringify({
          userId: member.userId,
          goal: cleanGoal,
          exercises: items.map((item) => ({
            exerciseId: item.exerciseId,
            dayOfWeek: item.dayOfWeek,
            sets: item.sets,
            reps: item.reps,
          })),
        }),
      })
      toast.success('Saved custom workout plan', { description: res.plan.goal })
      onSaved(res.plan)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>Build Custom Workout Plan</DialogTitle>
          <DialogDescription>
            Compose a custom day-by-day training routine for {member?.fullName || 'selected athlete'} ({member?.fitnessLevel || 'Member'}).
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={submit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="custom-plan-goal">Routine Title *</Label>
            <Input
              id="custom-plan-goal"
              placeholder="e.g., Upper / Lower Hypertrophy Split"
              maxLength={120}
              value={goal}
              onChange={(e) => setGoal(e.target.value)}
            />
          </div>

          <div className="rounded-xl border bg-muted/30 p-3.5 space-y-3">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Add Exercise Row
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-12 gap-2.5 items-end">
              <div className="sm:col-span-3 space-y-1">
                <Label className="text-xs">Day</Label>
                <Select value={dayOfWeek} onValueChange={setDayOfWeek}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {DAYS.map((d) => (
                      <SelectItem key={d} value={d}>{d}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="sm:col-span-4 space-y-1">
                <Label className="text-xs">Exercise</Label>
                <Select value={exerciseId} onValueChange={setExerciseId}>
                  <SelectTrigger className="w-full"><SelectValue placeholder="Choose exercise" /></SelectTrigger>
                  <SelectContent>
                    {exercises.map((ex) => (
                      <SelectItem key={ex.exerciseId} value={String(ex.exerciseId)}>
                        {ex.name} ({ex.muscleGroup})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="sm:col-span-2 space-y-1">
                <Label className="text-xs">Sets</Label>
                <Input
                  type="number"
                  min={1}
                  max={20}
                  className="tabular-nums"
                  value={sets}
                  onChange={(e) => setSets(e.target.value)}
                />
              </div>
              <div className="sm:col-span-2 space-y-1">
                <Label className="text-xs">Reps</Label>
                <Input
                  placeholder="10-12"
                  maxLength={25}
                  value={reps}
                  onChange={(e) => setReps(e.target.value)}
                />
              </div>
              <div className="sm:col-span-1">
                <Button type="button" onClick={addDraftExercise} className="w-full cursor-pointer active:scale-[0.97]">
                  <Plus className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>

          <div className="rounded-xl border overflow-hidden">
            <ScrollArea className={items.length > 4 ? 'h-56' : 'max-h-56'}>
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/40 hover:bg-muted/40">
                    <TableHead>Day</TableHead>
                    <TableHead>Exercise</TableHead>
                    <TableHead>Muscle</TableHead>
                    <TableHead className="text-right">Sets × Reps</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((item) => (
                    <TableRow key={item.key}>
                      <TableCell className="font-medium">{item.dayOfWeek}</TableCell>
                      <TableCell className="font-medium">{item.exerciseName}</TableCell>
                      <TableCell className="text-muted-foreground">{item.muscleGroup}</TableCell>
                      <TableCell className="text-right font-mono text-xs tabular-nums">
                        {item.sets} × {item.reps}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => removeDraftExercise(item.key)}
                          className="text-rose-600 cursor-pointer"
                          aria-label={`Remove ${item.exerciseName}`}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {items.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="py-8 text-center text-xs text-muted-foreground">
                        No exercises added yet. Use the row builder above to compose the routine.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </ScrollArea>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={saving || items.length === 0} className="cursor-pointer active:scale-[0.97]">
              {saving ? 'Saving Plan...' : `Save Custom Plan (${items.length})`}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
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
      <Card className="py-3 px-4 flex flex-row flex-wrap items-center justify-between gap-4 shadow-none">
        <div className="flex flex-wrap items-center gap-6">
          <StudioSwitch
            id="filter-machines"
            checked={showMachines}
            onCheckedChange={setShowMachines}
            label="Gym Machines"
          />
          <StudioSwitch
            id="filter-freeweights"
            checked={showFree}
            onCheckedChange={setShowFree}
            label="Free Weights & Bodyweight"
          />
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={!selected} onClick={() => { setEditing(selected); setModalOpen(true) }}>
            <Pencil className="h-3.5 w-3.5" />
            Edit
          </Button>
          <Button variant="outline" size="sm" disabled={!selected} onClick={() => selected && remove(selected)} className="text-rose-600">
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
        <Card className="xl:col-span-8 p-0 overflow-hidden shadow-none">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/40 hover:bg-muted/40">
                <TableHead className="w-16">ID</TableHead>
                <TableHead>Workout</TableHead>
                <TableHead>Muscle</TableHead>
                <TableHead>Difficulty</TableHead>
                <TableHead>Station</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((ex) => (
                <TableRow
                  key={ex.exerciseId}
                  onClick={() => setSelectedId(ex.exerciseId)}
                  className={cn(
                    'cursor-pointer',
                    selected?.exerciseId === ex.exerciseId && 'bg-muted/70'
                  )}
                >
                  <TableCell className="font-mono text-xs text-muted-foreground">#{ex.exerciseId}</TableCell>
                  <TableCell className="font-medium">{ex.name}</TableCell>
                  <TableCell className="text-muted-foreground">{ex.muscleGroup}</TableCell>
                  <TableCell className="text-muted-foreground">{ex.difficulty}</TableCell>
                  <TableCell>
                    <Badge variant="outline" className="max-w-[180px] truncate block">
                      {ex.equipment.replace('Machine: ', '')}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
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
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit Exercise' : 'Add New Exercise'}</DialogTitle>
          <DialogDescription>Configure station requirements and target muscle group.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-3.5">
          <div className="space-y-1">
            <Label>Workout Name *</Label>
            <Input value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>Muscle Group</Label>
              <Select value={form.muscleGroup || 'Chest'} onValueChange={(v) => setForm({ ...form, muscleGroup: v })}>
                <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {['Chest', 'Back', 'Legs', 'Shoulders', 'Arms', 'Core', 'Full Body'].map((g) => (
                    <SelectItem key={g} value={g}>{g}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Difficulty</Label>
              <Select value={form.difficulty || 'Beginner'} onValueChange={(v) => setForm({ ...form, difficulty: v })}>
                <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {['Beginner', 'Intermediate', 'Advanced'].map((d) => (
                    <SelectItem key={d} value={d}>{d}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="space-y-1">
            <Label>Equipment</Label>
            <Input value={form.equipment || ''} onChange={(e) => setForm({ ...form, equipment: e.target.value })} />
          </div>
          <div className="space-y-1">
            <Label>Execution Cues *</Label>
            <textarea
              rows={3}
              className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
              value={form.description || ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit">Save Exercise</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/* ============================================================================
   4. EXPIRY RADAR & STAFF ACCOUNTS
   ============================================================================ */
function ExpiryAlertsView({ members, onRenewMember }) {
  const alerts = members.filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7))

  return (
    <div className="space-y-5">
      <div className="border-b pb-5">
        <h1 className="text-2xl font-bold tracking-tight">Expiry &amp; Renewal Radar</h1>
        <p className="text-xs text-muted-foreground mt-0.5">
          Athletes whose 1-month pass is overdue or expiring within 7 days
        </p>
      </div>

      <Card className="p-0 overflow-hidden shadow-none">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40 hover:bg-muted/40">
              <TableHead>Athlete</TableHead>
              <TableHead>Phone</TableHead>
              <TableHead>Emergency Contact</TableHead>
              <TableHead>End Date</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Desk Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {alerts.map((a) => (
              <TableRow key={a.userId}>
                <TableCell className="font-medium">{a.fullName}</TableCell>
                <TableCell className="font-mono text-xs tabular-nums text-muted-foreground">{a.contactNo}</TableCell>
                <TableCell className="text-xs text-muted-foreground">
                  {a.emergencyContactName} ({a.emergencyContactNo})
                </TableCell>
                <TableCell className="font-mono text-xs tabular-nums">{a.endDate}</TableCell>
                <TableCell>
                  <span
                    className={cn(
                      'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-medium border tabular-nums',
                      a.status !== 'Active'
                        ? 'border-rose-500/25 bg-rose-500/10 text-rose-700 dark:text-rose-400'
                        : 'border-amber-500/25 bg-amber-500/10 text-amber-700 dark:text-amber-400'
                    )}
                  >
                    <span
                      className={cn(
                        'h-1.5 w-1.5 rounded-full',
                        a.status !== 'Active' ? 'bg-rose-500' : 'bg-amber-500'
                      )}
                    />
                    {a.balanceLabel}
                  </span>
                </TableCell>
                <TableCell className="text-right">
                  <Button size="sm" variant="outline" onClick={() => onRenewMember(a)} className="cursor-pointer">
                    <RefreshCw className="h-3.5 w-3.5" />
                    Renew +1 Mo
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {alerts.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-12 text-center text-sm text-muted-foreground">
                  All athlete passes are currently healthy.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
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
      toast.success(`Created operator account @${form.username}`)
      setOpen(false)
      setForm({ username: '', password: '', role: 'Staff', firstName: '', lastName: '', contactNo: '09' })
      load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const remove = async (u) => {
    if (u.userId === currentUser.userId) return toast.error('Cannot delete your own active Admin session.')
    if (!window.confirm(`Delete @${u.username}?`)) return
    await api(`/api/staff/${u.userId}`, { method: 'DELETE' })
    toast.success(`Deleted @${u.username}`)
    load()
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Staff &amp; Operator Accounts</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Provision Admin and Front Desk Staff console credentials
          </p>
        </div>
        <Button size="sm" onClick={() => setOpen(true)} className="cursor-pointer">
          <Plus className="h-3.5 w-3.5" />
          Add Operator
        </Button>
      </div>

      <Card className="p-0 overflow-hidden shadow-none">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40 hover:bg-muted/40">
              <TableHead className="w-16">ID</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Username</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Contact</TableHead>
              <TableHead className="text-right">Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((u) => (
              <TableRow key={u.userId}>
                <TableCell className="font-mono text-xs text-muted-foreground">#{u.userId}</TableCell>
                <TableCell className="font-medium">{u.fullName}</TableCell>
                <TableCell className="font-mono text-xs text-muted-foreground">@{u.username}</TableCell>
                <TableCell>
                  <Badge variant={u.role === 'Admin' ? 'default' : u.role === 'Staff' ? 'secondary' : 'outline'}>
                    {u.role}
                  </Badge>
                </TableCell>
                <TableCell className="font-mono text-xs tabular-nums text-muted-foreground">{u.contactNo}</TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="icon-sm" onClick={() => remove(u)} className="text-rose-600 cursor-pointer">
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Operator Account</DialogTitle>
            <DialogDescription>Grant Admin or Front Desk Staff console access.</DialogDescription>
          </DialogHeader>
          <form onSubmit={create} className="space-y-3.5">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Username *</Label>
                <Input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
              </div>
              <div className="space-y-1">
                <Label>Password *</Label>
                <Input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
              </div>
              <div className="space-y-1">
                <Label>First Name *</Label>
                <Input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
              </div>
              <div className="space-y-1">
                <Label>Last Name *</Label>
                <Input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
              </div>
              <div className="space-y-1">
                <Label>Role</Label>
                <Select value={form.role} onValueChange={(v) => setForm({ ...form, role: v })}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Staff">Staff</SelectItem>
                    <SelectItem value="Admin">Admin</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Contact No. *</Label>
                <Input value={form.contactNo} onChange={(e) => setForm({ ...form, contactNo: e.target.value })} />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit">Create Account</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
