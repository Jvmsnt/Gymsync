import React from 'react'
import { Cpu, Dumbbell } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

export function MusclePreviewCard({ exercise }) {
  if (!exercise) {
    return (
      <Card className="shadow-none min-h-[400px] items-center justify-center text-center p-6">
        <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center mb-2">
          <Dumbbell className="h-5 w-5 text-muted-foreground" />
        </div>
        <CardTitle className="text-sm">No Exercise Selected</CardTitle>
        <CardDescription className="text-xs max-w-[220px] mt-1">
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

  const activeFill = '#10b981'
  const mutedFill = 'currentColor'

  return (
    <Card className="shadow-none py-5 gap-4">
      <CardHeader className="px-5 flex-row items-start justify-between gap-2 space-y-0">
        <div>
          <p className="text-[11px] font-mono uppercase tracking-wider text-muted-foreground">
            {exercise.muscleGroup} · {exercise.difficulty}
          </p>
          <CardTitle className="text-base mt-1">{exercise.name}</CardTitle>
        </div>
        <Badge variant="secondary" className="shrink-0">
          {isMachine ? <Cpu className="h-3 w-3 text-sky-500" /> : <Dumbbell className="h-3 w-3 text-emerald-500" />}
          {isMachine ? 'Machine' : 'Free / Body'}
        </Badge>
      </CardHeader>

      <Separator />

      <CardContent className="px-5 space-y-4">
        <div className="rounded-xl border bg-muted/40 p-4 flex items-center justify-between gap-4">
          <svg viewBox="0 0 140 190" className="w-28 h-40 text-zinc-300 dark:text-zinc-700 shrink-0">
            <circle cx="70" cy="22" r="13" fill={mutedFill} />
            <rect x="48" y="40" width="44" height="58" rx="10" fill={mutedFill} />
            <rect
              x="30"
              y="43"
              width="13"
              height="50"
              rx="6"
              fill={isArm || isFull ? activeFill : mutedFill}
            />
            <rect
              x="97"
              y="43"
              width="13"
              height="50"
              rx="6"
              fill={isArm || isFull ? activeFill : mutedFill}
            />
            <circle cx="48" cy="46" r="8" fill={isShoulder || isFull ? activeFill : mutedFill} />
            <circle cx="92" cy="46" r="8" fill={isShoulder || isFull ? activeFill : mutedFill} />
            <rect
              x="53"
              y="45"
              width="34"
              height="20"
              rx="6"
              fill={isChest || isBack || isFull ? activeFill : mutedFill}
            />
            <rect
              x="55"
              y="69"
              width="30"
              height="24"
              rx="5"
              fill={isCore || isFull ? activeFill : mutedFill}
            />
            <rect
              x="51"
              y="102"
              width="16"
              height="74"
              rx="7"
              fill={isLeg || isFull ? activeFill : mutedFill}
            />
            <rect
              x="73"
              y="102"
              width="16"
              height="74"
              rx="7"
              fill={isLeg || isFull ? activeFill : mutedFill}
            />
          </svg>

          <div className="space-y-3 text-xs flex-1">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                Target Zone
              </span>
              <p className="font-semibold text-sm mt-0.5 text-emerald-600 dark:text-emerald-400">
                {exercise.muscleGroup}
              </p>
            </div>
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                Equipment Station
              </span>
              <p className="font-medium mt-0.5">{exercise.equipment}</p>
            </div>
          </div>
        </div>

        <div>
          <span className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
            Execution &amp; Coaching Cues
          </span>
          <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{exercise.description}</p>
        </div>
      </CardContent>
    </Card>
  )
}
