import { Bell } from 'lucide-react'
import { useDeviceAttribute, useHubVariable } from '../../store/deviceStore'

interface Props { deviceId: string; label: string; lrpHubVarName: string }

function formatLastSeen(ts: number): { text: string; color: string } {
  if (!ts || ts === 0) return { text: 'Never detected', color: 'text-gray-400' }
  const diffMs = Date.now() - ts * 1000
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  if (diffSec < 60) return { text: 'Just now', color: 'text-amber-500' }
  if (diffMin < 5) return { text: `${diffMin}m ago`, color: 'text-yellow-500' }
  if (diffMin < 60) return { text: `${diffMin}m ago`, color: 'text-gray-400' }
  const diffHr = Math.floor(diffMin / 60)
  return { text: `${diffHr}h ago`, color: 'text-gray-400' }
}

export function RingDetectionTile({ deviceId, label, lrpHubVarName }: Props) {
  const switchState = useDeviceAttribute(deviceId, 'switch')
  const lrpValue = useHubVariable(lrpHubVarName)
  const lrpTs = lrpValue !== undefined ? Number(lrpValue) : 0
  const { text, color } = formatLastSeen(lrpTs)
  const isTriggered = switchState === 'on'

  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 ${isTriggered ? 'border-amber-400' : 'border-gray-200 dark:border-gray-700'}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-2">{label}</p>
      <div className={`flex items-center gap-2 ${isTriggered ? 'text-amber-500' : 'text-gray-400'}`}>
        <Bell size={20} fill={isTriggered ? 'currentColor' : 'none'} />
        <span className="font-semibold text-sm">{isTriggered ? 'Detected!' : 'Clear'}</span>
      </div>
      <p className={`text-xs mt-1 ${color}`}>{text}</p>
    </div>
  )
}
