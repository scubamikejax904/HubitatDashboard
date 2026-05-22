import { useState } from 'react'
import { Loader2 } from 'lucide-react'
import { useIsPending } from '../../store/deviceStore'
import { useCommand } from '../../hooks/useCommand'

interface Props { deviceId: string; label: string }

export function ButtonTile({ deviceId, label }: Props) {
  const isPending = useIsPending(deviceId)
  const [execute] = useCommand()
  const [pressed, setPressed] = useState(false)

  const handlePush = async () => {
    setPressed(true)
    await execute({ deviceId, command: 'push', value: '1' })
    setTimeout(() => setPressed(false), 500)
  }

  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-4 shadow-sm bg-white dark:bg-gray-800">
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate mb-3">{label}</p>
      <button
        onClick={handlePush}
        disabled={isPending}
        className={`px-4 py-2 rounded-lg font-medium text-sm transition-all active:scale-95 disabled:opacity-50 ${
          pressed
            ? 'bg-green-500 text-white'
            : 'bg-blue-500 text-white hover:bg-blue-600'
        }`}
      >
        {isPending ? <Loader2 size={14} className="animate-spin inline mr-1" /> : null}
        Push
      </button>
    </div>
  )
}
