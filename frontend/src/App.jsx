import React, { useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { AnimatePresence, motion } from 'motion/react'
import {
  Activity,
  ArrowUpRight,
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
  QrCode,
  RefreshCw,
  Search,
  Shield,
  ShieldCheck,
  Trash2,
  UserPlus,
  Users,
  Zap,
} from 'lucide-react'
import { Toaster } from '@/components/ui/sonner'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
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

function BrandMark({ className = 'h-8 w-8' }) {
  return (
    <svg viewBox="0 0 36 36" fill="none" className={cn('shrink-0', className)} aria-hidden="true">
      <rect width="36" height="36" rx="10" fill="#D4FF3F" />
      <path d="M10 22.5L15.5 11H21L17 18H26L14.5 27L17.5 20H10V22.5Z" fill="#0A0B0D" />
      <rect x="23" y="10" width="4" height="4" rx="1" fill="#0A0B0D" />
    </svg>
  )
}

export default function App() {
  const [dark, setDark] = useState(true)
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
          health={health}
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
      label: 'Training',
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

  const shiftLabel = new Date()
    .toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    .toUpperCase()

  return (
    <TooltipProvider>
      <div className="h-screen w-screen overflow-hidden flex bg-sidebar text-foreground">
        <Toaster position="bottom-right" theme={dark ? 'dark' : 'light'} />

        {/* Persistent Obsidian Sidebar Rail */}
        <aside className="w-62 shrink-0 flex flex-col justify-between p-4 select-none border-r border-border bg-sidebar">
          <div className="space-y-5">
            {/* Brand Lockup */}
            <div className="flex items-center justify-between px-1">
              <div className="flex items-center gap-3">
                <BrandMark className="h-8 w-8" />
                <div>
                  <p className="font-display text-sm font-bold leading-none tracking-tight">GymSync</p>
                  <p className="mt-1 font-mono text-[10px] text-muted-foreground">DESK OS · V2.0</p>
                </div>
              </div>
              <span className="rounded-md bg-[#D4FF3F]/15 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-volt">
                {isAdmin ? 'ADMIN' : 'STAFF'}
              </span>
            </div>

            {/* Sidebar Quick Command Lookup */}
            <button
              onClick={() => setCmdOpen(true)}
              className="w-full flex items-center justify-between gap-2 rounded-lg border border-border bg-card px-2.5 h-9 text-xs text-muted-foreground hover:border-input hover:text-foreground transition-colors cursor-pointer"
            >
              <span className="flex items-center gap-2">
                <Search className="h-3.5 w-3.5" />
                <span>Quick lookup...</span>
              </span>
              <kbd className="rounded border border-input bg-surface-3 px-1.5 py-0.5 font-mono text-[10px] text-foreground">
                ⌘K
              </kbd>
            </button>

            {/* Grouped Navigation */}
            <div className="space-y-4">
              {navGroups.map((group) => (
                <div key={group.label} className="space-y-1">
                  <p className="px-2.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground/75">
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
                            'relative w-full flex items-center justify-between rounded-lg px-3 py-2 text-[13px] font-medium transition-colors cursor-pointer',
                            active
                              ? 'text-foreground'
                              : 'text-muted-foreground hover:text-foreground hover:bg-surface-2/60'
                          )}
                        >
                          {active && (
                            <motion.span
                              layoutId="sidebarActiveNav"
                              transition={{ type: 'spring', stiffness: 460, damping: 36 }}
                              className="absolute inset-0 rounded-lg bg-surface-2 border border-input"
                            >
                              <span className="absolute left-0 top-1/2 -translate-y-1/2 h-4 w-[3px] rounded-r bg-volt" />
                            </motion.span>
                          )}
                          <span className="relative z-10 flex items-center gap-2.5">
                            <Icon className={cn('h-4 w-4 shrink-0', active && 'text-volt')} />
                            <span>{item.label}</span>
                          </span>
                          {item.badge != null && (
                            <span
                              className={cn(
                                'relative z-10 rounded-md px-1.5 py-0.5 font-mono text-[10px] tabular-nums',
                                item.alert
                                  ? 'bg-[#FF5C7A]/15 text-[#FF5C7A] font-semibold'
                                  : 'bg-surface-3 text-muted-foreground'
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

          {/* Bottom Dock */}
          <div className="space-y-2.5 pt-3 border-t border-border">
            <div className="px-3 py-2 rounded-lg bg-card border border-border">
              <StudioSwitch
                id="sidebar-theme-switch"
                checked={dark}
                onCheckedChange={setDark}
                label="Obsidian Volt"
                description={dark ? 'Obsidian desk canvas' : 'Daylight theme'}
              />
            </div>

            <Tooltip>
              <TooltipTrigger asChild>
                <div className="flex items-center justify-between rounded-lg border border-border bg-card px-3 py-2 font-mono text-[11px] text-muted-foreground cursor-default">
                  <span className="flex items-center gap-2">
                    <Activity className="h-3.5 w-3.5 text-[#3DDC97]" />
                    <span>{health.dbLabel}</span>
                  </span>
                  <span className="h-1.5 w-1.5 rounded-full bg-[#3DDC97]" />
                </div>
              </TooltipTrigger>
              <TooltipContent side="right">
                {health.jdbcConnected ? 'Connected via JDBC' : 'Running with seeded in-memory dataset'}
              </TooltipContent>
            </Tooltip>

            <BaseActionMenu
              trigger={
                <div className="flex items-center justify-between gap-2 rounded-xl border border-input bg-surface-2 p-2.5 hover:bg-surface-3 transition-colors cursor-pointer">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <Avatar className="h-8 w-8 border border-input">
                      <AvatarFallback className="bg-surface-3 font-mono text-[11px] font-semibold">
                        {operatorInitials}
                      </AvatarFallback>
                    </Avatar>
                    <div className="min-w-0">
                      <p className="truncate text-xs font-semibold text-foreground">{user.fullName}</p>
                      <p className="truncate font-mono text-[10px] text-muted-foreground">@{user.username} · Desk</p>
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

        {/* Rounded Inset Main Workspace Shell */}
        <div className="flex-1 p-2.5 pl-2 flex flex-col overflow-hidden">
          <section className="flex-1 rounded-xl border border-border bg-background flex flex-col overflow-hidden">
            {/* Top Context Bar (56px) */}
            <header className="h-14 shrink-0 border-b border-border bg-card px-6 flex items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span>GymSync</span>
                <span className="font-mono">/</span>
                <span className="font-medium text-foreground">{currentNav?.label}</span>
              </div>

              <div className="flex items-center gap-2.5">
                <span className="hidden md:inline-flex items-center gap-1.5 rounded-full bg-[#D4FF3F]/15 px-2.5 py-1 font-mono text-[11px] font-medium text-volt">
                  <span className="h-1.5 w-1.5 rounded-full bg-volt" />
                  SHIFT LIVE · {shiftLabel}
                </span>

                {isAdmin && (
                  <a href="/api/reports/csv" download>
                    <Button variant="outline" size="sm" className="border-input bg-surface-2 hover:bg-surface-3 cursor-pointer">
                      <Download className="h-3.5 w-3.5" />
                      Export CSV
                    </Button>
                  </a>
                )}

                <Button
                  size="sm"
                  onClick={() => {
                    setEditingMember(null)
                    setMemberModalOpen(true)
                  }}
                  className="cursor-pointer active:scale-[0.97]"
                >
                  <Plus className="h-3.5 w-3.5" />
                  Register member
                </Button>

                <Button variant="ghost" size="icon-sm" onClick={loadWorkspaceData} title="Sync roster" className="cursor-pointer">
                  <RefreshCw className="h-3.5 w-3.5" />
                </Button>
              </div>
            </header>

            {/* Scrollable Workspace Content */}
            <main className="flex-1 overflow-y-auto px-7 py-6">
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
        </div>

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
   1. OBSIDIAN + ELECTRIC VOLT SPLIT SIGN-IN (Matches Figma Screen 01)
   ============================================================================ */
function LoginView({ dark, health, onToggleDark, onSuccess }) {
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
      {/* Left 7 cols: Obsidian Architectural Showcase */}
      <div className="hidden lg:flex lg:col-span-7 relative overflow-hidden flex-col justify-between bg-[#09090B] px-14 py-12 text-[#F4F4F1] border-r border-[#1F2228]">
        <Spotlight className="-top-40 left-0 md:left-32 md:-top-24" fill="#D4FF3F" />
        <DotPattern
          width={24}
          height={24}
          cr={1}
          className="text-[#2E323A]/85 [mask-image:radial-gradient(640px_circle_at_center,white,transparent)]"
        />

        {/* Brand Header */}
        <div className="relative z-10 flex items-center gap-3">
          <BrandMark className="h-9 w-9" />
          <div>
            <p className="font-display text-sm font-bold tracking-tight text-[#F4F4F1]">GYMSYNC</p>
            <p className="font-mono text-[10px] text-[#666973]">DESK OS · V2.0</p>
          </div>
        </div>

        {/* Center Hero + Floating Pass Showcase */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: 'easeOut' }}
          className="relative z-10 max-w-2xl space-y-8"
        >
          <div className="space-y-3">
            <span className="inline-flex items-center gap-2 rounded-full border border-[#D4FF3F]/35 bg-[#D4FF3F]/10 px-3 py-1 font-mono text-[11px] font-medium text-[#D4FF3F]">
              <span className="h-1.5 w-1.5 rounded-full bg-[#D4FF3F]" />
              FRONT DESK &amp; WORKOUT OPERATIONS
            </span>
            <h1 className="font-display text-5xl font-extrabold tracking-tight leading-[1.04] text-[#F4F4F1]">
              Fast desk check-ins.
            </h1>
            <p className="font-serif-accent text-4xl text-[#D4FF3F] leading-none">
              Zero spreadsheet guesswork.
            </p>
            <p className="pt-1 text-sm text-[#A0A2AA] leading-relaxed max-w-[48ch]">
              Verify athlete passes in milliseconds, automate 1-month renewals, monitor expiring members, and coach structured training plans from one fast operator console.
            </p>
          </div>

          {/* Live Pass Preview Card + Telemetry Chips */}
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 items-center">
            <div className="xl:col-span-8 relative rounded-2xl border border-[#2E323A] obsidian-pass-surface p-5 overflow-hidden shadow-2xl">
              <div className="space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono text-[10px] uppercase tracking-[0.14em] text-[#D4FF3F]">
                    GymSync Studio Access · 1-Mo
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full bg-[#3DDC97]/15 px-2.5 py-0.5 font-mono text-[11px] font-medium text-[#3DDC97]">
                    <ShieldCheck className="h-3 w-3" />
                    Entry Cleared
                  </span>
                </div>
                <p className="font-display text-xl font-bold text-[#F4F4F1]">Carlos Mendoza</p>
                <p className="font-mono text-[11px] text-[#A0A2AA]">PASS #0003 · @carlos_m · BEGINNER</p>
              </div>

              <div className="my-3.5 h-px bg-[#2E323A]/70" />

              <div className="flex items-end justify-between">
                <div>
                  <p className="text-[10px] uppercase tracking-[0.1em] text-[#666973]">Cycle Balance</p>
                  <div className="flex items-baseline gap-1.5 mt-0.5">
                    <span className="font-display text-3xl font-bold text-[#D4FF3F] tabular-nums">18</span>
                    <span className="text-xs text-[#A0A2AA]">days left</span>
                  </div>
                </div>
                <div className="flex items-center gap-2.5">
                  <div className="text-right">
                    <p className="text-[10px] uppercase tracking-[0.1em] text-[#666973]">Valid Thru</p>
                    <p className="font-mono text-xs text-[#F4F4F1]">2026-12-09</p>
                  </div>
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-[#2E323A] bg-[#202329]">
                    <QrCode className="h-5 w-5 text-[#D4FF3F]" />
                  </div>
                </div>
              </div>

              <div className="mt-3.5 space-y-1.5">
                <div className="h-1.5 w-full rounded-full bg-[#202329] overflow-hidden">
                  <div
                    className="h-full w-3/5 rounded-full"
                    style={{ background: 'linear-gradient(90deg, #3DDC97 0%, #D4FF3F 100%)' }}
                  />
                </div>
                <div className="flex justify-between font-mono text-[10px] text-[#666973]">
                  <span>START 2026-11-09</span>
                  <span className="text-[#A0A2AA]">60% REMAINING</span>
                </div>
              </div>
              <BorderBeam size={110} duration={7} colorFrom="#D4FF3F" colorTo="#3DDC97" />
            </div>

            <div className="xl:col-span-4 space-y-2.5">
              {[
                ['SCAN LATENCY', '11 ms', 'text-[#D4FF3F]'],
                ['PASS CYCLE', '30 Days', 'text-[#F4F4F1]'],
                ['ACCESS SCOPE', 'Desk Only', 'text-[#3DDC97]'],
              ].map(([k, v, cls]) => (
                <div key={k} className="rounded-xl border border-[#1F2228] bg-[#121317] px-4 py-3">
                  <p className="text-[10px] font-medium uppercase tracking-[0.08em] text-[#666973]">{k}</p>
                  <p className={cn('mt-0.5 text-sm font-semibold', cls)}>{v}</p>
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        <div className="relative z-10 flex items-center justify-between font-mono text-[11px] text-[#666973]">
          <span>DIT 2-3 GROUP 3 · JAVA + MYSQL + REACT DESK OS</span>
          <span>ADMIN &amp; STAFF OPERATORS ONLY</span>
        </div>
      </div>

      {/* Right 5 cols: Operator Sign-In Panel */}
      <div className="lg:col-span-5 flex flex-col justify-between p-8 sm:p-12 bg-card">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 lg:hidden">
            <BrandMark className="h-7 w-7" />
            <span className="font-display font-bold">GYMSYNC</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-[#3DDC97]/15 px-2.5 py-1 font-mono text-[11px] font-medium text-[#3DDC97]">
              <span className="h-1.5 w-1.5 rounded-full bg-[#3DDC97]" />
              {health.dbLabel}
            </span>
            <StudioSwitch id="login-theme" checked={dark} onCheckedChange={onToggleDark} />
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          className="mx-auto w-full max-w-sm space-y-6"
        >
          <div>
            <h2 className="font-display text-2xl font-bold tracking-tight">Operator Sign In</h2>
            <p className="mt-1.5 text-sm text-muted-foreground">
              Authenticate with your Admin or Front Desk Staff credentials to open the operations console.
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
              <Label htmlFor="login-user" className="text-xs text-muted-foreground">
                Username
              </Label>
              <Input
                id="login-user"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="admin or staff"
                className="h-11 bg-surface-2 border-input"
                autoFocus
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="login-pass" className="text-xs text-muted-foreground">
                Password
              </Label>
              <div className="relative">
                <Input
                  id="login-pass"
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  className="h-11 pr-10 bg-surface-2 border-input"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-3 top-3 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  {showPass ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" size="lg" className="w-full h-11 font-semibold cursor-pointer active:scale-[0.98]" disabled={loading}>
              <ArrowUpRight className="h-4 w-4" />
              {loading ? 'Authenticating...' : 'Open Desk Console'}
            </Button>
          </form>

          <div className="flex items-center gap-3">
            <div className="h-px flex-1 bg-border" />
            <span className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
              1-Click Demo Access
            </span>
            <div className="h-px flex-1 bg-border" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => submitLogin('admin', 'admin123')}
              className="rounded-xl border border-input bg-surface-2 p-3.5 text-left hover:bg-surface-3 hover:border-volt/50 transition-colors cursor-pointer"
            >
              <div className="flex items-center gap-2 text-xs font-semibold text-foreground">
                <Shield className="h-4 w-4 text-volt" />
                Admin Desk
              </div>
              <p className="mt-1 text-[11px] text-muted-foreground">Full roster &amp; staff</p>
            </button>

            <button
              type="button"
              onClick={() => submitLogin('staff', 'staff123')}
              className="rounded-xl border border-input bg-surface-2 p-3.5 text-left hover:bg-surface-3 hover:border-[#3DDC97]/50 transition-colors cursor-pointer"
            >
              <div className="flex items-center gap-2 text-xs font-semibold text-foreground">
                <Users className="h-4 w-4 text-[#3DDC97]" />
                Staff Desk
              </div>
              <p className="mt-1 text-[11px] text-muted-foreground">Check-in &amp; renewals</p>
            </button>
          </div>
        </motion.div>

        <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
          <Shield className="h-3.5 w-3.5" />
          <span>Member accounts are roster-only and blocked from operator sign-in.</span>
        </div>
      </div>
    </div>
  )
}

/* ============================================================================
   2. DIRECTORY VIEW (Matches Figma Screen 03 Background)
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
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-mono font-semibold uppercase tracking-[0.12em] text-volt">
            Athlete Roster · {members.length} Members
          </p>
          <h1 className="font-display mt-1 text-2xl font-bold tracking-tight">Member Directory</h1>
        </div>
        <Button size="sm" onClick={onNewMember} className="cursor-pointer active:scale-[0.97]">
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
            className="pl-9 bg-card border-input"
          />
        </div>

        <Tabs value={filter} onValueChange={setFilter}>
          <TabsList className="bg-card border border-border">
            <TabsTrigger value="all">All ({members.length})</TabsTrigger>
            <TabsTrigger value="active">Active</TabsTrigger>
            <TabsTrigger value="expiring">Expiring ≤7d</TabsTrigger>
            <TabsTrigger value="expired">Expired</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <Card className="p-0 overflow-hidden border-border bg-card shadow-none">
        <Table>
          <TableHeader>
            <TableRow className="bg-surface-2 hover:bg-surface-2">
              <TableHead className="w-16 font-mono text-[10px] uppercase tracking-wider">ID</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Athlete</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Contact</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Tier</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Cycle Period</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Balance</TableHead>
              <TableHead className="text-right font-mono text-[10px] uppercase tracking-wider">Status</TableHead>
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
                  className="cursor-pointer hover:bg-[#D4FF3F]/6 transition-colors"
                >
                  <TableCell className="font-mono text-xs text-muted-foreground tabular-nums">
                    #{String(m.userId).padStart(3, '0')}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2.5">
                      <Avatar className="h-8 w-8 border border-input">
                        <AvatarFallback className="bg-surface-3 font-mono text-[10px] font-semibold">
                          {m.initials}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="font-medium text-sm leading-none">{m.fullName}</p>
                        <p className="font-mono text-[11px] text-muted-foreground mt-1">@{m.username}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="font-mono text-xs tabular-nums text-muted-foreground">
                    {m.contactNo}
                  </TableCell>
                  <TableCell>
                    <span className="rounded-md bg-surface-3 px-2 py-0.5 font-mono text-[11px] text-muted-foreground">
                      {m.fitnessLevel}
                    </span>
                  </TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground tabular-nums">
                    {m.startDate} → {m.endDate}
                  </TableCell>
                  <TableCell className="font-mono text-xs font-medium tabular-nums">{m.balanceLabel}</TableCell>
                  <TableCell className="text-right">
                    <span
                      className={cn(
                        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 font-mono text-[11px] font-medium',
                        isAct && !isExpSoon && 'bg-[#3DDC97]/15 text-[#3DDC97]',
                        isExpSoon && 'bg-[#FFB547]/15 text-[#FFB547]',
                        !isAct && 'bg-[#FF5C7A]/15 text-[#FF5C7A]'
                      )}
                    >
                      <span
                        className={cn(
                          'h-1.5 w-1.5 rounded-full',
                          isAct && !isExpSoon ? 'bg-[#3DDC97]' : isExpSoon ? 'bg-[#FFB547]' : 'bg-[#FF5C7A]'
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
      <DialogContent className="sm:max-w-xl bg-card border-input">
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
   3. WORKOUTS & MUSCLE STUDIO (Matches Figma Screen 04 + Preserves CustomPlanModal)
   ============================================================================ */
function WorkoutsHubView({ isAdmin, members }) {
  return (
    <Tabs defaultValue="coach" className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-mono font-semibold uppercase tracking-[0.12em] text-volt">
            Rule-Based Training Engine
          </p>
          <h1 className="font-display mt-1 text-2xl font-bold tracking-tight">Workouts &amp; Muscle Studio</h1>
        </div>
        <TabsList className="bg-card border border-border">
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
      <Card className="py-4 px-5 border-border bg-card shadow-none">
        <div className="flex flex-wrap gap-3 items-end">
          <div className="min-w-[180px] flex-[1.3] space-y-1">
            <Label className="text-[10px] font-mono uppercase tracking-wider text-muted-foreground">Athlete</Label>
            <Select
              value={memberId}
              onValueChange={(uid) => {
                setMemberId(uid)
                const next = members.find((m) => String(m.userId) === String(uid))
                if (next?.fitnessLevel) setLevel(next.fitnessLevel)
              }}
            >
              <SelectTrigger className="w-full bg-surface-2 border-input"><SelectValue placeholder="Choose member" /></SelectTrigger>
              <SelectContent>
                {members.map((m) => (
                  <SelectItem key={m.userId} value={String(m.userId)}>
                    #{m.userId} — {m.fullName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="min-w-[155px] flex-1 space-y-1">
            <Label className="text-[10px] font-mono uppercase tracking-wider text-muted-foreground">Goal</Label>
            <Select value={goal} onValueChange={setGoal}>
              <SelectTrigger className="w-full bg-surface-2 border-input"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['Weight Loss', 'Muscle Gain', 'General Fitness', 'Strength Foundation'].map((g) => (
                  <SelectItem key={g} value={g}>{g}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="min-w-[125px] flex-[0.8] space-y-1">
            <Label className="text-[10px] font-mono uppercase tracking-wider text-muted-foreground">Level</Label>
            <Select value={level} onValueChange={setLevel}>
              <SelectTrigger className="w-full bg-surface-2 border-input"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['Beginner', 'Intermediate', 'Advanced'].map((l) => (
                  <SelectItem key={l} value={l}>{l}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="min-w-[90px] w-24 space-y-1">
            <Label className="text-[10px] font-mono uppercase tracking-wider text-muted-foreground">Split</Label>
            <Select value={daysPerWeek} onValueChange={setDaysPerWeek}>
              <SelectTrigger className="w-full bg-surface-2 border-input"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['2', '3', '4', '5'].map((d) => (
                  <SelectItem key={d} value={d}>{d}d/wk</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="min-w-[140px] flex-1 space-y-1">
            <Label className="text-[10px] font-mono uppercase tracking-wider text-muted-foreground">Equipment Zone</Label>
            <Select value={zone} onValueChange={setZone}>
              <SelectTrigger className="w-full bg-surface-2 border-input"><SelectValue /></SelectTrigger>
              <SelectContent>
                {['Full Gym', 'Gym Machines Only', 'Dumbbells', 'Bodyweight'].map((z) => (
                  <SelectItem key={z} value={z}>{z}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex gap-2 shrink-0">
            <Button onClick={generate} disabled={generating || !memberId} className="cursor-pointer active:scale-[0.97]">
              <Zap className="h-3.5 w-3.5" />
              {generating ? 'Saving...' : 'Generate'}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={!memberId || exercises.length === 0}
              onClick={() => setCustomOpen(true)}
              className="border-input bg-surface-2 hover:bg-surface-3 cursor-pointer active:scale-[0.97]"
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
                <SelectTrigger className="w-full bg-card border-input">
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
              variant="ghost"
              size="sm"
              disabled={!activePlan}
              onClick={() => setDeleteDialogOpen(true)}
              className="bg-[#FF5C7A]/10 text-[#FF5C7A] hover:bg-[#FF5C7A]/20 hover:text-[#FF5C7A] cursor-pointer active:scale-[0.97]"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete Plan
            </Button>
          </div>

          <Card className="p-0 overflow-hidden border-border bg-card shadow-none">
            <Table>
              <TableHeader>
                <TableRow className="bg-surface-2 hover:bg-surface-2">
                  <TableHead className="font-mono text-[10px] uppercase tracking-wider">Day</TableHead>
                  <TableHead className="font-mono text-[10px] uppercase tracking-wider">Exercise</TableHead>
                  <TableHead className="font-mono text-[10px] uppercase tracking-wider">Muscle</TableHead>
                  <TableHead className="font-mono text-[10px] uppercase tracking-wider">Station</TableHead>
                  <TableHead className="text-right font-mono text-[10px] uppercase tracking-wider">Sets × Reps</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(activePlan?.exercises || []).map((item, idx) => {
                  const fullEx = exercises.find((e) => e.name.toLowerCase() === item.exerciseName.toLowerCase()) || null
                  const defaultFirst = !selectedEx && idx === 0
                  const isRowSelected = (selectedEx && fullEx && selectedEx.exerciseId === fullEx.exerciseId) || defaultFirst
                  const isMach = (item.equipment || '').toLowerCase().includes('machine')
                  return (
                    <TableRow
                      key={idx}
                      onClick={() => setSelectedEx(fullEx)}
                      className={cn(
                        'cursor-pointer transition-colors',
                        isRowSelected ? 'bg-[#D4FF3F]/10 hover:bg-[#D4FF3F]/15' : 'hover:bg-surface-2/70'
                      )}
                    >
                      <TableCell className={cn('font-mono text-xs', isRowSelected ? 'text-volt font-semibold' : 'text-muted-foreground')}>
                        {item.dayOfWeek}
                      </TableCell>
                      <TableCell className={cn('font-medium', isRowSelected && 'text-volt')}>
                        {item.exerciseName}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{item.muscleGroup}</TableCell>
                      <TableCell>
                        <span
                          className={cn(
                            'inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 font-mono text-[11px] font-medium max-w-[130px]',
                            isMach ? 'bg-[#6CC6FF]/15 text-[#6CC6FF]' : 'bg-[#3DDC97]/15 text-[#3DDC97]'
                          )}
                        >
                          <span className={cn('h-1.5 w-1.5 rounded-full shrink-0', isMach ? 'bg-[#6CC6FF]' : 'bg-[#3DDC97]')} />
                          <span className="truncate">{(item.equipment || '').replace('Machine: ', '')}</span>
                        </span>
                      </TableCell>
                      <TableCell className="text-right font-mono text-xs tabular-nums whitespace-nowrap">
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
        <DialogContent className="sm:max-w-md bg-card border-input">
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
      <DialogContent className="sm:max-w-3xl bg-card border-input">
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

          <div className="rounded-xl border border-border bg-surface-2 p-3.5 space-y-3">
            <p className="text-[11px] font-mono font-semibold uppercase tracking-wider text-muted-foreground">
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

          <div className="rounded-xl border border-border overflow-hidden">
            <ScrollArea className={items.length > 4 ? 'h-56' : 'max-h-56'}>
              <Table>
                <TableHeader>
                  <TableRow className="bg-surface-2 hover:bg-surface-2">
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
                          className="text-[#FF5C7A] cursor-pointer"
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
      <Card className="py-3 px-4 flex flex-row flex-wrap items-center justify-between gap-4 border-border bg-card shadow-none">
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
          <Button
            variant="ghost"
            size="sm"
            disabled={!selected}
            onClick={() => selected && remove(selected)}
            className="bg-[#FF5C7A]/10 text-[#FF5C7A] hover:bg-[#FF5C7A]/20 hover:text-[#FF5C7A]"
          >
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
        <Card className="xl:col-span-8 p-0 overflow-hidden border-border bg-card shadow-none">
          <Table>
            <TableHeader>
              <TableRow className="bg-surface-2 hover:bg-surface-2">
                <TableHead className="w-12 font-mono text-[10px] uppercase tracking-wider">ID</TableHead>
                <TableHead className="font-mono text-[10px] uppercase tracking-wider">Workout</TableHead>
                <TableHead className="font-mono text-[10px] uppercase tracking-wider">Muscle</TableHead>
                <TableHead className="font-mono text-[10px] uppercase tracking-wider">Difficulty</TableHead>
                <TableHead className="font-mono text-[10px] uppercase tracking-wider">Station</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((ex) => {
                const isSelected = selected?.exerciseId === ex.exerciseId
                const isMach = (ex.equipment || '').toLowerCase().includes('machine')
                return (
                  <TableRow
                    key={ex.exerciseId}
                    onClick={() => setSelectedId(ex.exerciseId)}
                    className={cn(
                      'cursor-pointer transition-colors',
                      isSelected ? 'bg-[#D4FF3F]/10 hover:bg-[#D4FF3F]/15' : 'hover:bg-surface-2/70'
                    )}
                  >
                    <TableCell className={cn('font-mono text-xs', isSelected ? 'text-volt' : 'text-muted-foreground')}>
                      #{ex.exerciseId}
                    </TableCell>
                    <TableCell className={cn('font-medium', isSelected && 'text-volt')}>{ex.name}</TableCell>
                    <TableCell className="text-muted-foreground">{ex.muscleGroup}</TableCell>
                    <TableCell className="text-muted-foreground">{ex.difficulty}</TableCell>
                    <TableCell>
                      <span
                        className={cn(
                          'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 font-mono text-[11px] font-medium max-w-[140px]',
                          isMach ? 'bg-[#6CC6FF]/15 text-[#6CC6FF]' : 'bg-[#3DDC97]/15 text-[#3DDC97]'
                        )}
                      >
                        <span className={cn('h-1.5 w-1.5 rounded-full shrink-0', isMach ? 'bg-[#6CC6FF]' : 'bg-[#3DDC97]')} />
                        <span className="truncate">{ex.equipment.replace('Machine: ', '')}</span>
                      </span>
                    </TableCell>
                  </TableRow>
                )
              })}
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
      <DialogContent className="bg-card border-input">
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
              className="w-full rounded-md border border-input bg-surface-2 px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
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
   4. EXPIRY RADAR & STAFF ACCOUNTS (Matches Figma Screen 05 Background)
   ============================================================================ */
function ExpiryAlertsView({ members, onRenewMember }) {
  const alerts = members.filter((m) => m.status !== 'Active' || (m.daysRemaining >= 0 && m.daysRemaining <= 7))

  return (
    <div className="space-y-5">
      <div>
        <p className="text-[11px] font-mono font-semibold uppercase tracking-[0.12em] text-[#FFB547]">
          Proactive Retention Radar
        </p>
        <h1 className="font-display mt-1 text-2xl font-bold tracking-tight">Expiry &amp; Renewal Radar</h1>
        <p className="text-xs text-muted-foreground mt-0.5">
          Athletes whose 1-month pass is overdue or expiring within 7 days
        </p>
      </div>

      <Card className="p-0 overflow-hidden border-border bg-card shadow-none">
        <Table>
          <TableHeader>
            <TableRow className="bg-surface-2 hover:bg-surface-2">
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Athlete</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Contact</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Emergency Contact</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">End Date</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Status</TableHead>
              <TableHead className="text-right font-mono text-[10px] uppercase tracking-wider">Desk Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {alerts.map((a) => (
              <TableRow key={a.userId} className="hover:bg-surface-2/60">
                <TableCell className="font-medium">{a.fullName}</TableCell>
                <TableCell className="font-mono text-xs tabular-nums text-muted-foreground">{a.contactNo}</TableCell>
                <TableCell className="text-xs text-muted-foreground">
                  {a.emergencyContactName} ({a.emergencyContactNo})
                </TableCell>
                <TableCell className="font-mono text-xs tabular-nums">{a.endDate}</TableCell>
                <TableCell>
                  <span
                    className={cn(
                      'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 font-mono text-[11px] font-medium tabular-nums',
                      a.status !== 'Active'
                        ? 'bg-[#FF5C7A]/15 text-[#FF5C7A]'
                        : 'bg-[#FFB547]/15 text-[#FFB547]'
                    )}
                  >
                    <span
                      className={cn(
                        'h-1.5 w-1.5 rounded-full',
                        a.status !== 'Active' ? 'bg-[#FF5C7A]' : 'bg-[#FFB547]'
                      )}
                    />
                    {a.balanceLabel}
                  </span>
                </TableCell>
                <TableCell className="text-right">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => onRenewMember(a)}
                    className="border-input bg-surface-2 hover:bg-surface-3 cursor-pointer"
                  >
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
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-mono font-semibold uppercase tracking-[0.12em] text-volt">
            Access &amp; Permissions
          </p>
          <h1 className="font-display mt-1 text-2xl font-bold tracking-tight">Staff &amp; Operator Accounts</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Provision Admin and Front Desk Staff console credentials
          </p>
        </div>
        <Button size="sm" onClick={() => setOpen(true)} className="cursor-pointer active:scale-[0.97]">
          <Plus className="h-3.5 w-3.5" />
          Add Operator
        </Button>
      </div>

      <Card className="p-0 overflow-hidden border-border bg-card shadow-none">
        <Table>
          <TableHeader>
            <TableRow className="bg-surface-2 hover:bg-surface-2">
              <TableHead className="w-16 font-mono text-[10px] uppercase tracking-wider">ID</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Name</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Username</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Role</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Contact</TableHead>
              <TableHead className="text-right font-mono text-[10px] uppercase tracking-wider">Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((u) => (
              <TableRow key={u.userId} className="hover:bg-surface-2/60">
                <TableCell className="font-mono text-xs text-muted-foreground">#{u.userId}</TableCell>
                <TableCell className="font-medium">{u.fullName}</TableCell>
                <TableCell className="font-mono text-xs text-muted-foreground">@{u.username}</TableCell>
                <TableCell>
                  <span
                    className={cn(
                      'inline-flex items-center rounded-full px-2.5 py-0.5 font-mono text-[11px] font-medium',
                      u.role === 'Admin' && 'bg-[#D4FF3F]/15 text-volt',
                      u.role === 'Staff' && 'bg-[#6CC6FF]/15 text-[#6CC6FF]',
                      u.role !== 'Admin' && u.role !== 'Staff' && 'bg-surface-3 text-muted-foreground'
                    )}
                  >
                    {u.role}
                  </span>
                </TableCell>
                <TableCell className="font-mono text-xs tabular-nums text-muted-foreground">{u.contactNo}</TableCell>
                <TableCell className="text-right">
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => remove(u)}
                    className="text-[#FF5C7A] hover:bg-[#FF5C7A]/12 cursor-pointer"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="bg-card border-input">
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
