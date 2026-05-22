import { Power, Loader2 } from 'lucide-react'
import { useDeviceAttribute, useIsPending } from '../../store/deviceStore'
import { useCommand } from '../../hooks/useCommand'

interface Props { deviceId: string; label: string }

export function SwitchTile({ deviceId, label }: Props) {
  const switchState = useDeviceAttribute(deviceId, 'switch')
  const isPending = useIsPending(deviceId)
  const [execute] = useCommand()

  const toggle = () => {
    const next = switchState === 'on' ? 'off' : 'on'
    execute({
      deviceId,
      command: next,
      optimisticAttribute: 'switch',
      optimisticValue: next,
    })
  }

  if (switchState === undefined) {
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-sm">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
        <div className="animate-pulse h-8 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    )
  }

  const isOn = switchState === 'on'
  return (
    <div className={`rounded-xl border p-4 shadow-sm bg-white dark:bg-gray-800 transition-colors ${isOn ? 'border-green-400' : 'border-gray-200 dark:border-gray-700'}`}>
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
      <button
        onClick={toggle}
        disabled={isPending}
        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg font-medium text-sm transition-all active:scale-95 disabled:opacity-50 ${
          isOn
            ? 'bg-green-500 text-white hover:bg-green-600'
            : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
        }`}
        aria-label={`${label} ${isOn ? 'on' : 'off'}`}
      >
        {isPending ? <Loader2 size={16} className="animate-spin" /> : <Power size={16} />}
        <span>{isOn ? 'On' : 'Off'}</span>
      </button>
    </div>
  )
}
