import { DoorOpen, DoorClosed } from 'lucide-react'
import { useDeviceAttribute } from '../../store/deviceStore'

interface Props { deviceId: string; label: string }

export function ContactTile({ deviceId, label }: Props) {
  const contact = useDeviceAttribute(deviceId, 'contact')

  if (contact === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const isOpen = contact === 'open'
  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${isOpen ? 'border-orange-400' : 'border-green-400'}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${isOpen ? 'text-orange-500' : 'text-green-500'}`}>
        {isOpen ? <DoorOpen size={24} /> : <DoorClosed size={24} />}
        <span className="font-semibold">{isOpen ? 'Open' : 'Closed'}</span>
      </div>
    </div>
  )
}
