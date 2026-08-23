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

export function WaterisonTile({ deviceId: propDeviceId, label, hubVarName }: Props) {
  const resolvedByLabel = useDeviceIdByLabel(label)
  const deviceId = propDeviceId || resolvedByLabel
  const switchState = useDeviceAttribute(deviceId, 'switch')
  const hubVarValue = useHubVariable(hubVarName ?? '')
  const [execute] = useCommand()
  const [secondsLeft, setSecondsLeft] = useState<number | null>(null)
  const [startTime, setStartTime] = useState<number | null>(null)

  const toggle = () => {
    const next = switchState === 'on' ? 'off' : 'on'
    execute({ deviceId, command: next, optimisticAttribute: 'switch', optimisticValue: next })
    
    // When turning off the switch, reset the timer state
    if (next === 'off') {
      setSecondsLeft(null)
      setStartTime(null)
    }
  }

  const isOn = switchState === 'on'

  useEffect(() => {
    // Only start timer when switch is on and we have a valid hubVarValue
    if (!isOn || !hubVarName) {
      setSecondsLeft(null)
      setStartTime(null)
      return
    }

    // If we don't yet have a valid hub variable value, wait for it
    if (hubVarValue === undefined || hubVarValue === null) {
      setSecondsLeft(null)
      return
    }

    const totalSeconds = Number(hubVarValue) * 60

    // Set the start time for this specific tile instance when we have the first valid value
    if (startTime === null) {
      setStartTime(Date.now())
    }

    const update = () => {
      if (startTime === null) return;
      const elapsed = Math.floor((Date.now() - startTime) / 1000)
      const newSecondsLeft = Math.max(0, totalSeconds - elapsed)
      setSecondsLeft(newSecondsLeft)
    }

    update()
    const interval = setInterval(update, 1000)
    return () => clearInterval(interval)
  }, [isOn, hubVarName, hubVarValue, startTime])

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
        {showAutoOff && (
          secondsLeft > 0 
            ? <span className="text-[11px] font-normal mt-0.5">Auto-off: {formatCountdown(secondsLeft)}</span>
            : <span className="text-[11px] font-normal mt-0.5">Timer Has Completed</span>
        )}
      </div>
    </button>
  )
}