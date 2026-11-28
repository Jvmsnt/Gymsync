import React from 'react'
import { Cpu, Dumbbell } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

export function MusclePreviewCard({ exercise }) {
  if (!exercise) {
    return (
      <Card className="border-border bg-card shadow-none min-h-[420px] items-center justify-center text-center p-6">
        <div className="h-10 w-10 rounded-full bg-surface-2 border border-border flex items-center justify-center mb-2">
          <Dumbbell className="h-5 w-5 text-muted-foreground" />
        </div>
        <CardTitle className="text-sm">No Exercise Selected</CardTitle>
        <CardDescription className="text-xs max-w-[230px] mt-1">
          Select an exercise row to inspect target muscle anatomy, equipment station, and coaching cues.
        </CardDescription>
      </Card>
    )
  }

  const mg = (exercise.muscleGroup || '').toLowerCase()
  const isMachine = (exercise.equipment || '').toLowerCase().includes('machine')

  const isChest = mg.includes('chest')
  const isBack = mg.includes('back')
  const isShoulder = mg.includes('shoulder')
  const isArm = mg.includes('arm')
  const isLeg = mg.includes('leg')
  const isCore = mg.includes('core')
  const isFull = !isChest && !isBack && !isShoulder && !isArm && !isLeg && !isCore

  const primaryVolt = '#D4FF3F'
  const secondaryMint = '#3DDC97'
  const baseFill = '#202329'
  const baseStroke = '#2E323A'

  return (
    <Card className="border-border bg-card shadow-none py-5 gap-4">
      <CardHeader className="px-5 flex flex-row items-start justify-between gap-2 space-y-0">
        <div>
          <p className="text-[10px] font-mono font-semibold uppercase tracking-[0.1em] text-volt">
            {exercise.muscleGroup} · {exercise.difficulty} · {isMachine ? 'Machine' : 'Free / Body'}
          </p>
          <CardTitle className="text-base mt-1">{exercise.name}</CardTitle>
        </div>
        <span className="shrink-0 inline-flex items-center gap-1.5 rounded-full bg-[#6CC6FF]/15 px-2.5 py-1 font-mono text-[11px] font-medium text-[#6CC6FF]">
          {isMachine ? <Cpu className="h-3 w-3" /> : <Dumbbell className="h-3 w-3" />}
          {isMachine ? 'Station' : 'Free'}
        </span>
      </CardHeader>

      <Separator />

      <CardContent className="px-5 space-y-4">
        {/* Anatomy Visual Stage */}
        <div className="rounded-xl border border-border bg-background p-4 flex items-center justify-between gap-3.5">
          <svg viewBox="0 0 144 214" className="w-24 h-36 shrink-0">
            <circle cx="72" cy="24" r="14" fill={baseFill} stroke={baseStroke} strokeWidth="1.5" />
            <rect x="46" y="44" width="52" height="68" rx="14" fill={baseFill} stroke={baseStroke} strokeWidth="1.5" />
            {/* Upper Torso / Chest & Back */}
            <path
              d="M50 48 H94 L88 82 H56 L50 48 Z"
              fill={isChest || isBack || isFull ? primaryVolt : baseFill}
              stroke={isChest || isBack || isFull ? primaryVolt : 'none'}
            />
            {/* Core */}
            <rect
              x="56"
              y="84"
              width="32"
              height="24"
              rx="6"
              fill={isCore || isFull ? primaryVolt : baseFill}
            />
            {/* Shoulders */}
            <circle
              cx="44"
              cy="54"
              r="9"
              fill={isShoulder || isFull ? primaryVolt : isBack || isChest ? secondaryMint : baseFill}
              fillOpacity={isBack || isChest ? 0.7 : 1}
              stroke={baseStroke}
              strokeWidth="1"
            />
            <circle
              cx="100"
              cy="54"
              r="9"
              fill={isShoulder || isFull ? primaryVolt : isBack || isChest ? secondaryMint : baseFill}
              fillOpacity={isBack || isChest ? 0.7 : 1}
              stroke={baseStroke}
              strokeWidth="1"
            />
            {/* Arms */}
            <rect
              x="26"
              y="54"
              width="14"
              height="56"
              rx="7"
              fill={isArm || isFull ? primaryVolt : baseFill}
              stroke={baseStroke}
              strokeWidth="1.5"
            />
            <rect
              x="104"
              y="54"
              width="14"
              height="56"
              rx="7"
              fill={isArm || isFull ? primaryVolt : baseFill}
              stroke={baseStroke}
              strokeWidth="1.5"
            />
            {/* Legs */}
            <rect
              x="52"
              y="118"
              width="18"
              height="86"
              rx="9"
              fill={isLeg || isFull ? primaryVolt : baseFill}
              stroke={baseStroke}
              strokeWidth="1.5"
            />
            <rect
              x="74"
              y="118"
              width="18"
              height="86"
              rx="9"
              fill={isLeg || isFull ? primaryVolt : baseFill}
              stroke={baseStroke}
              strokeWidth="1.5"
            />
          </svg>

          <div className="space-y-3.5 text-xs flex-1 min-w-0">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                Primary Target
              </span>
              <p className="font-semibold text-sm mt-0.5 text-volt">{exercise.muscleGroup}</p>
            </div>
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                Equipment Station
              </span>
              <p className="font-medium mt-0.5 text-foreground break-words">{exercise.equipment}</p>
            </div>
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                Difficulty Tier
              </span>
              <p className="font-mono text-xs mt-0.5 text-[#3DDC97]">{exercise.difficulty}</p>
            </div>
          </div>
        </div>

        {/* Coaching Cues Box */}
        <div className="rounded-xl border border-border bg-surface-2 p-4 space-y-1.5">
          <span className="text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
            Execution &amp; Coaching Cues
          </span>
          <p className="text-xs leading-relaxed text-muted-foreground">{exercise.description}</p>
        </div>
      </CardContent>
    </Card>
  )
}
