import { Thermometer } from 'lucide-react'
import { useDeviceAttribute } from '../../store/deviceStore'

interface Props { deviceId: string; label: string }

export function TemperatureTile({ deviceId, label }: Props) {
  const temp = useDeviceAttribute(deviceId, 'temperature')
  const humidity = useDeviceAttribute(deviceId, 'humidity')

  if (temp === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const t = Number(temp)
  const color = t < 65 ? 'text-blue-500' : t > 80 ? 'text-orange-500' : 'text-green-500'
  const borderColor = t < 65 ? 'border-blue-300' : t > 80 ? 'border-orange-300' : 'border-green-300'

  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${borderColor}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${color}`}>
        <Thermometer size={24} />
        <span className="text-2xl font-bold">{t.toFixed(1)}</span>
        <span className="text-sm font-medium">°F</span>
      </div>
      {humidity !== undefined && (
        <p className="text-xs text-gray-400 mt-1">{Number(humidity).toFixed(0)}% humidity</p>
      )}
    </div>
  )
}
