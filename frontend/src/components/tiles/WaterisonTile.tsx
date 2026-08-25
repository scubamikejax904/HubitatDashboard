import { useEffect, useState } from 'react'
import { useDeviceAttribute, useDeviceIdByLabel, useHubVariable } from '../../store/deviceStore'
import { useCommand } from '../../hooks/useCommand'

interface Props { deviceId: string; label: string; hubVarName?: string }

/** Global map of timer start times keyed by hubVarName. Persists across component unmounts
 *  so the countdown doesn't restart from full when navigating away from and back to a group page. */
const timerStartTimes = new Map<string, number>()

function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

/** Special water-status tile: a switch with an ALWAYS-VISIBLE countdown.
 *  Unlike ConnectorSwitchTile, the remaining auto-off time is shown prominently
 *  (large, centered) rather than as a small sub-label. */
export function WaterisonTile({ deviceId: propDeviceId, label, hubVarName }: Props) {
  const resolvedByLabel = useDeviceIdByLabel(label)
  const deviceId = propDeviceId || resolvedByLabel
  const switchState = useDeviceAttribute(deviceId, 'switch')
  // Default to the standard water auto-off variable when none is configured for this tile
  const varName = hubVarName ?? 'WaterTimeout'
  const hubVarValue = useHubVariable(varName)
  const [execute] = useCommand()
  const [secondsLeft, setSecondsLeft] = useState<number | null>(null)

  const toggle = () => {
    const next = switchState === 'on' ? 'off' : 'on'
    execute({ deviceId, command: next, optimisticAttribute: 'switch', optimisticValue: next })
    if (next === 'off') {
      setSecondsLeft(null)
      timerStartTimes.delete(varName)
    }
  }

  const isOn = switchState === 'on'

  useEffect(() => {
    if (!isOn || hubVarValue === undefined || hubVarValue === null) {
      if (!isOn) timerStartTimes.delete(varName)
      setSecondsLeft(null)
      return
    }

    const totalSeconds = Number(hubVarValue) * 60

    if (!timerStartTimes.has(varName)) {
      timerStartTimes.set(varName, Date.now())
    }
    const startTime = timerStartTimes.get(varName)!

    const update = () => {
      const elapsed = Math.floor((Date.now() - startTime) / 1000)
      setSecondsLeft(Math.max(0, totalSeconds - elapsed))
    }

    update()
    const interval = setInterval(update, 1000)
    return () => clearInterval(interval)
  }, [isOn, varName, hubVarValue])

  // Blue palette for a water-themed tile
  const badgeClass = isOn
    ? 'bg-blue-600 text-white border-blue-400'
    : 'bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400'

  const countdownVisible = isOn && secondsLeft !== null

  return (
    <button
      onClick={toggle}
      className={`rounded-lg px-3 py-2 text-xs font-semibold transition-all active:scale-95 text-left w-full ${badgeClass}`}
      aria-pressed={isOn}
      aria-label={`${label}: ${isOn ? 'on' : 'off'}`}
    >
      <div className="flex flex-col leading-tight">
        <span>{label}</span>
        {countdownVisible && (
          secondsLeft > 0 ? (
            <>
              <span className="text-xl font-bold tabular-nums leading-tight mt-1">
                {formatCountdown(secondsLeft)}
              </span>
              <span className="text-[10px] font-normal opacity-80">until auto-off</span>
            </>
          ) : (
            <span className="text-[11px] font-normal mt-1">Timer Has Completed</span>
          )
        )}
        {isOn && secondsLeft === null && (
          <span className="text-[10px] font-normal opacity-80 mt-1">waiting for timer…</span>
        )}
      </div>
    </button>
  )
}
