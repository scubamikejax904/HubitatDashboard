import { PersonStanding } from 'lucide-react'
import { useDeviceAttribute } from '../../store/deviceStore'

interface Props { deviceId: string; label: string }

export function MotionTile({ deviceId, label }: Props) {
  const motion = useDeviceAttribute(deviceId, 'motion')

  if (motion === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const isActive = motion === 'active'
  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${isActive ? 'border-amber-400' : 'border-gray-200 dark:border-gray-700'}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${isActive ? 'text-amber-500' : 'text-gray-400'}`}>
        <PersonStanding size={24} />
        <span className="font-semibold">{isActive ? 'Active' : 'Inactive'}</span>
      </div>
    </div>
  )
}
