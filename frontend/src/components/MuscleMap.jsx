import React from 'react'
import { Badge, Card } from './ui'
import { Cpu, Dumbbell } from 'lucide-react'

export function MusclePreviewCard({ exercise }) {
  if (!exercise) {
    return (
      <Card className="p-6 flex flex-col items-center justify-center text-center min-h-[420px]">
        <div className="h-10 w-10 rounded-full bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center mb-3">
          <Dumbbell className="h-5 w-5 text-zinc-400" />
        </div>
        <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">No Exercise Selected</p>
        <p className="text-xs text-zinc-500 mt-1 max-w-[220px]">
          Select an exercise row to inspect target muscle anatomy, equipment type, and coaching cues.
        </p>
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
    <Card className="p-5 flex flex-col gap-4">
      <div className="flex items-start justify-between gap-3 border-b border-zinc-200 dark:border-zinc-800 pb-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-wider text-zinc-400">
            {exercise.muscleGroup} • {exercise.difficulty}
          </p>
          <h3 className="text-base font-semibold text-zinc-900 dark:text-zinc-50 mt-0.5">{exercise.name}</h3>
        </div>
        <Badge tone={isMachine ? 'blue' : 'emerald'}>
          {isMachine ? <Cpu className="h-3 w-3" /> : <Dumbbell className="h-3 w-3" />}
          {isMachine ? 'Machine' : 'Free / Body'}
        </Badge>
      </div>

      <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/60 p-4 flex items-center justify-between gap-4">
        <svg viewBox="0 0 140 190" className="w-32 h-44 text-zinc-300 dark:text-zinc-700 shrink-0">
          {/* Head */}
          <circle cx="70" cy="22" r="13" fill={mutedFill} />
          {/* Torso */}
          <rect x="48" y="40" width="44" height="58" rx="10" fill={mutedFill} />
          {/* Arms */}
          <rect
            x="30"
            y="43"
            width="13"
            height="50"
            rx="6"
            fill={isArm || isFull ? activeFill : mutedFill}
            opacity={isArm || isFull ? 0.9 : 1}
          />
          <rect
            x="97"
            y="43"
            width="13"
            height="50"
            rx="6"
            fill={isArm || isFull ? activeFill : mutedFill}
            opacity={isArm || isFull ? 0.9 : 1}
          />
          {/* Shoulders */}
          {(isShoulder || isFull) && (
            <>
              <circle cx="42" cy="48" r="9" fill={activeFill} opacity="0.9" />
              <circle cx="98" cy="48" r="9" fill={activeFill} opacity="0.9" />
            </>
          )}
          {/* Chest */}
          {(isChest || isFull) && (
            <>
              <rect x="51" y="44" width="17" height="19" rx="4" fill={activeFill} opacity="0.9" />
              <rect x="72" y="44" width="17" height="19" rx="4" fill={activeFill} opacity="0.9" />
            </>
          )}
          {/* Back */}
          {isBack && (
            <polygon points="50,43 90,43 82,82 58,82" fill={activeFill} opacity="0.9" />
          )}
          {/* Core */}
          {(isCore || isFull) && (
            <rect x="59" y="66" width="22" height="28" rx="5" fill={activeFill} opacity="0.9" />
          )}
          {/* Legs */}
          <rect
            x="50"
            y="102"
            width="16"
            height="68"
            rx="7"
            fill={isLeg || isFull ? activeFill : mutedFill}
            opacity={isLeg || isFull ? 0.9 : 1}
          />
          <rect
            x="74"
            y="102"
            width="16"
            height="68"
            rx="7"
            fill={isLeg || isFull ? activeFill : mutedFill}
            opacity={isLeg || isFull ? 0.9 : 1}
          />
        </svg>

        <div className="flex-1 space-y-3 text-xs">
          <div>
            <p className="font-semibold uppercase tracking-wider text-zinc-400 text-[10px]">Primary Target</p>
            <p className="text-sm font-semibold text-emerald-600 dark:text-emerald-400 mt-0.5">{exercise.muscleGroup}</p>
          </div>
          <div>
            <p className="font-semibold uppercase tracking-wider text-zinc-400 text-[10px]">Equipment</p>
            <p className="text-zinc-700 dark:text-zinc-200 font-medium mt-0.5">{exercise.equipment}</p>
          </div>
          <div>
            <p className="font-semibold uppercase tracking-wider text-zinc-400 text-[10px]">Difficulty</p>
            <p className="text-zinc-700 dark:text-zinc-200 font-medium mt-0.5">{exercise.difficulty}</p>
          </div>
        </div>
      </div>

      <div>
        <p className="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">Execution Cues</p>
        <p className="text-sm leading-relaxed text-zinc-600 dark:text-zinc-300">{exercise.description}</p>
      </div>
    </Card>
  )
}
