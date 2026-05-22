import { useRef } from 'react'
import { Power, Loader2 } from 'lucide-react'
import { useDeviceAttribute, useIsPending } from '../../store/deviceStore'
import { useCommand } from '../../hooks/useCommand'
import { useCollapsed } from '../../hooks/useCollapsed'
import { CollapsibleCard } from './CollapsibleCard'

interface Props { deviceId: string; label: string }

function debounceRef(fn: (v: number) => void, delay: number) {
  let timer: ReturnType<typeof setTimeout>
  return (v: number) => { clearTimeout(timer); timer = setTimeout(() => fn(v), delay) }
}

function hslToHex(h: number, s: number): string {
  const hDeg = h * 3.6
  const sNorm = s / 100
  const lNorm = 0.5
  const c = (1 - Math.abs(2 * lNorm - 1)) * sNorm
  const x = c * (1 - Math.abs((hDeg / 60) % 2 - 1))
  const m = lNorm - c / 2
  let r = 0, g = 0, b = 0
  if (hDeg < 60) { r=c; g=x } else if (hDeg < 120) { r=x; g=c } else if (hDeg < 180) { g=c; b=x }
  else if (hDeg < 240) { g=x; b=c } else if (hDeg < 300) { r=x; b=c } else { r=c; b=x }
  const toHex = (n: number) => Math.round((n + m) * 255).toString(16).padStart(2, '0')
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

export function RGBWTile({ deviceId, label }: Props) {
  const switchState = useDeviceAttribute(deviceId, 'switch')
  const level = Number(useDeviceAttribute(deviceId, 'level') ?? 100)
  const hue = Number(useDeviceAttribute(deviceId, 'hue') ?? 0)
  const sat = Number(useDeviceAttribute(deviceId, 'saturation') ?? 100)
  const ct = Number(useDeviceAttribute(deviceId, 'colorTemperature') ?? 4000)
  const isPending = useIsPending(deviceId)
  const [execute] = useCommand()
  const [collapsed, toggleCollapsed] = useCollapsed(`rgbw-${deviceId}`)

  const sendLevel = useRef(debounceRef((v) => execute({ deviceId, command: 'setLevel', value: String(v) }), 400)).current
  const sendHue = useRef(debounceRef((v) => execute({ deviceId, command: 'setHue', value: String(v) }), 400)).current
  const sendSat = useRef(debounceRef((v) => execute({ deviceId, command: 'setSaturation', value: String(v) }), 400)).current
  const sendCt = useRef(debounceRef((v) => execute({ deviceId, command: 'setColorTemperature', value: String(v) }), 400)).current

  const isOn = switchState === 'on'
  const swatchColor = hslToHex(hue, sat)

  const header = (
    <div className="flex items-center gap-2 min-w-0">
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate flex-1">{label}</p>
      <div className="w-4 h-4 rounded-full border border-gray-300 flex-shrink-0" style={{ backgroundColor: swatchColor }} />
      <button
        onClick={(e) => { e.stopPropagation(); execute({ deviceId, command: isOn ? 'off' : 'on', optimisticAttribute: 'switch', optimisticValue: isOn ? 'off' : 'on' }) }}
        disabled={isPending}
        className={`flex items-center gap-1 px-2 py-0.5 rounded-lg text-xs font-medium transition-all disabled:opacity-50 flex-shrink-0 ${isOn ? 'bg-purple-500 text-white' : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'}`}
      >
        {isPending ? <Loader2 size={10} className="animate-spin" /> : <Power size={10} />}
        {isOn ? 'On' : 'Off'}
      </button>
    </div>
  )

  return (
    <CollapsibleCard
      collapsed={collapsed}
      onToggle={toggleCollapsed}
      header={header}
      borderClass={isOn ? 'border-purple-400' : 'border-gray-200 dark:border-gray-700'}
      className="col-span-2"
    >
      <div className="space-y-2">
        {[
          { label: 'Brightness', value: level, min: 0, max: 100, unit: '%', send: sendLevel },
          { label: 'Hue', value: hue, min: 0, max: 100, unit: '°', send: sendHue },
          { label: 'Saturation', value: sat, min: 0, max: 100, unit: '%', send: sendSat },
        ].map(({ label: l, value, min, max, unit, send }) => (
          <div key={l} className="flex items-center gap-2">
            <span className="text-xs text-gray-400 w-20 shrink-0">{l}</span>
            <input type="range" min={min} max={max} defaultValue={value}
              onChange={(e) => send(Number(e.target.value))}
              className="flex-1 accent-purple-500" aria-label={l} />
            <span className="text-xs text-gray-400 w-8 text-right">{value}{unit}</span>
          </div>
        ))}
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-400 w-20 shrink-0">Color Temp</span>
          <input type="range" min={2700} max={6500} defaultValue={ct}
            onChange={(e) => sendCt(Number(e.target.value))}
            className="flex-1 accent-orange-400" aria-label="Color temperature" />
          <span className="text-xs text-gray-400 w-8 text-right">{ct}K</span>
        </div>
      </div>
    </CollapsibleCard>
  )
}

