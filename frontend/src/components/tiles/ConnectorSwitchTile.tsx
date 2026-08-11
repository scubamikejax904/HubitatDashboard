import { useEffect, useState } from 'react'
import { useDeviceAttribute, useDeviceIdByLabel, useHubVariable } from '../../store/deviceStore'
import { useCommand } from '../../hooks/useCommand'

interface Props { deviceId: string; label: string; hubVarName?: string }

function getBadgeColors(label: string, isOn: boolean): string {
  if (!isOn) return 'bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
  const l = label.toLowerCase()
  if (l.includes('alarm') || l.includes('alert') || l.includes('panic') || l.includes('trigger')) {
    return 'bg-red-500 text-white'
  }
  if (l.includes('silent') || l.includes('pause')) return 'bg-orange-400 text-white'
  if (l.includes('travel') || l.includes('away') || l.includes('pto')) return 'bg-blue-500 text-white'
  if (l.includes('holiday') || l.includes('christmas')) return 'bg-purple-500 text-white'
  return 'bg-green-500 text-white'
}

function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

/** Global map of timer start times keyed by hubVarName. Persists across component unmounts
 *  so the countdown doesn't restart when navigating away from and back to a group page. */
const timerStartTimes = new Map<string, number>()

export function ConnectorSwitchTile({ deviceId: propDeviceId, label, hubVarName }: Props) {
  const resolvedByLabel = useDeviceIdByLabel(label)
  const deviceId = propDeviceId || resolvedByLabel
  const switchState = useDeviceAttribute(deviceId, 'switch')
  const hubVarValue = useHubVariable(hubVarName ?? '')
  const [execute] = useCommand()
  const [secondsLeft, setSecondsLeft] = useState<number | null>(null)

  const toggle = () => {
    const next = switchState === 'on' ? 'off' : 'on'
    execute({ deviceId, command: next, optimisticAttribute: 'switch', optimisticValue: next })
  }

  const isOn = switchState === 'on'

  useEffect(() => {
    if (!isOn || !hubVarName || hubVarValue === undefined) {
      // Switch turned off — clean up any stored start time for this timer
      timerStartTimes.delete(hubVarName ?? '')
      setSecondsLeft(null)
      return
    }

    const totalSeconds = Number(hubVarValue) * 60

    // Only set a new startTime if this is a fresh "on" transition or we don't have one yet.
    // This prevents restarting the timer when navigating back to the group.
    if (!timerStartTimes.has(hubVarName)) {
      timerStartTimes.set(hubVarName, Date.now())
    }
    const startTime = timerStartTimes.get(hubVarName)!

    const update = () => {
      const elapsed = Math.floor((Date.now() - startTime) / 1000)
      setSecondsLeft(Math.max(0, totalSeconds - elapsed))
    }

    update()
    const interval = setInterval(update, 1000)
    return () => clearInterval(interval)
  }, [isOn, hubVarName, hubVarValue])

  const showAutoOff = isOn && hubVarName && secondsLeft !== null
  const badgeClass = getBadgeColors(label, isOn)

  return (
    <button
      onClick={toggle}
      className={`rounded-lg px-3 py-2 text-xs font-semibold transition-all active:scale-95 text-left w-full ${badgeClass}`}
      aria-pressed={isOn}
      aria-label={`${label}: ${isOn ? 'on' : 'off'}`}
    >
      <div className="flex flex-col leading-tight">
        <span>{label}</span>
        {showAutoOff && <span className="text-[11px] font-normal mt-0.5">Auto-off: {formatCountdown(secondsLeft!)}</span>}
      </div>
    </button>
  )
}
