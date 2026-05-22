import { BatteryFull, BatteryMedium, BatteryLow, BatteryWarning } from 'lucide-react'
import { useDeviceAttribute } from '../../store/deviceStore'

interface Props { deviceId: string; label: string }

function batteryMeta(pct: number) {
  if (pct >= 75) return { Icon: BatteryFull,    color: 'text-green-500',  border: 'border-green-300',  text: 'Good' }
  if (pct >= 40) return { Icon: BatteryMedium,  color: 'text-yellow-500', border: 'border-yellow-300', text: 'OK' }
  if (pct >= 15) return { Icon: BatteryLow,     color: 'text-orange-500', border: 'border-orange-300', text: 'Low' }
  return           { Icon: BatteryWarning,       color: 'text-red-500',    border: 'border-red-400',    text: 'Critical' }
}

export function BatteryTile({ deviceId, label }: Props) {
  const raw = useDeviceAttribute(deviceId, 'battery')

  if (raw === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const pct = Math.round(Number(raw))
  const { Icon, color, border, text } = batteryMeta(pct)

  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${border}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${color}`}>
        <Icon size={24} />
        <span className="text-2xl font-bold">{pct}</span>
        <span className="text-sm font-medium">%</span>
      </div>
      <p className={`text-xs font-medium mt-1 ${color}`}>{text}</p>
    </div>
  )
}
