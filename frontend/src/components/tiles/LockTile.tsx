import { useState } from 'react'
import { Lock, Unlock, Loader2 } from 'lucide-react'
import { useDeviceAttribute, useIsPending } from '../../store/deviceStore'
import { PinModal } from '../PinModal'
import { showToast } from '../../utils/toast'
import { useCollapsed } from '../../hooks/useCollapsed'
import { CollapsibleCard } from './CollapsibleCard'

interface Props { deviceId: string; label: string }

export function LockTile({ deviceId, label }: Props) {
  const lockState = useDeviceAttribute(deviceId, 'lock')
  const isPending = useIsPending(deviceId)
  const [pinOpen, setPinOpen] = useState(false)
  const [pendingCommand, setPendingCommand] = useState<'lock' | 'unlock' | null>(null)
  const [collapsed, toggleCollapsed] = useCollapsed(`lock-${deviceId}`)

  const handleAction = (cmd: 'lock' | 'unlock') => {
    setPendingCommand(cmd)
    setPinOpen(true)
  }

  const handlePinConfirm = async (pin: string) => {
    setPinOpen(false)
    if (!pendingCommand) return
    try {
      const res = await fetch(`/api/devices/${deviceId}/${pendingCommand}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin }),
      })
      if (!res.ok) throw new Error(res.status === 403 ? 'Invalid PIN' : 'Command failed')
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Lock command failed', 'error')
    }
    setPendingCommand(null)
  }

  const isLocked = lockState === 'locked'

  const header = (
    <div className="flex items-center gap-2 min-w-0">
      <p className="text-xs font-medium text-gray-900 dark:text-white truncate flex-1">{label}</p>
      <div className={`flex items-center gap-1 flex-shrink-0 ${isLocked ? 'text-green-500' : 'text-red-500'}`}>
        {isLocked ? <Lock size={14} /> : <Unlock size={14} />}
        <span className="text-xs font-semibold">{lockState === undefined ? '—' : isLocked ? 'Locked' : 'Unlocked'}</span>
        {isPending && <Loader2 size={12} className="animate-spin text-gray-400" />}
      </div>
    </div>
  )

  return (
    <>
      <CollapsibleCard
        collapsed={collapsed}
        onToggle={toggleCollapsed}
        header={header}
        borderClass={isLocked ? 'border-green-400' : 'border-red-400'}
      >
        <div className="flex gap-2">
          <button onClick={() => handleAction('lock')} disabled={isPending}
            className="flex-1 py-1.5 rounded-lg bg-green-500 text-white text-xs font-medium hover:bg-green-600 active:scale-95 transition-all disabled:opacity-50">
            Lock
          </button>
          <button onClick={() => handleAction('unlock')} disabled={isPending}
            className="flex-1 py-1.5 rounded-lg bg-red-500 text-white text-xs font-medium hover:bg-red-600 active:scale-95 transition-all disabled:opacity-50">
            Unlock
          </button>
        </div>
      </CollapsibleCard>
      <PinModal isOpen={pinOpen} title={pendingCommand === 'lock' ? 'Lock Door' : 'Unlock Door'}
        onConfirm={handlePinConfirm} onCancel={() => { setPinOpen(false); setPendingCommand(null) }} />
    </>
  )
}

