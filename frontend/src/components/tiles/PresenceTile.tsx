import { Wifi, WifiOff } from 'lucide-react'
import { useDeviceAttribute } from '../../store/deviceStore'

interface Props { deviceId: string; label: string }

export function PresenceTile({ deviceId, label }: Props) {
  const presence = useDeviceAttribute(deviceId, 'presence')

  if (presence === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const isPresent = presence === 'present'
  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${isPresent ? 'border-green-400' : 'border-gray-200 dark:border-gray-700'}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${isPresent ? 'text-green-500' : 'text-gray-400'}`}>
        {isPresent ? <Wifi size={24} /> : <WifiOff size={24} />}
        <span className="font-semibold">{isPresent ? 'Home' : 'Away'}</span>
      </div>
    </div>
  )
}
