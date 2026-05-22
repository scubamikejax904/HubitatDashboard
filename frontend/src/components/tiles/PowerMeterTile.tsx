import { Zap } from 'lucide-react'
import { useDeviceAttribute } from '../../store/deviceStore'

interface Props { deviceId: string; label: string }

export function PowerMeterTile({ deviceId, label }: Props) {
  const power = useDeviceAttribute(deviceId, 'power')

  if (power === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const watts = Number(power)
  const color = watts > 500 ? 'text-red-500' : watts > 100 ? 'text-amber-500' : 'text-green-500'
  const borderColor = watts > 500 ? 'border-red-300' : watts > 100 ? 'border-amber-300' : 'border-green-300'

  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${borderColor}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${color}`}>
        <Zap size={22} />
        <span className="text-2xl font-bold">{watts.toFixed(1)}</span>
        <span className="text-sm font-medium">W</span>
      </div>
    </div>
  )
}
