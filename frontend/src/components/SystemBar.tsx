import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Shield, ShieldOff, Moon, Clock, Wifi, WifiOff, RefreshCw, Settings, Menu } from 'lucide-react'
import { useHsmStatus, useCurrentMode, useConnectionStatus, useDeviceStore } from '../store/deviceStore'
import { useCommand } from '../hooks/useCommand'
import { SettingsModal } from './SettingsModal'
import { LogViewerModal } from './LogViewerModal'

function HsmBadge() {
  const status = useHsmStatus()
  const colorMap: Record<string, string> = {
    armedAway: 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300',
    armedHome: 'bg-amber-100 text-amber-700 dark:bg-amber-900 dark:text-amber-300',
    armedNight: 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300',
    disarmed: 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300',
    allDisarmed: 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300',
  }
  const iconMap: Record<string, React.ReactNode> = {
    armedAway: <Shield size={12} />,
    armedHome: <Shield size={12} />,
    armedNight: <Moon size={12} />,
    disarmed: <ShieldOff size={12} />,
    allDisarmed: <ShieldOff size={12} />,
  }
  const color = colorMap[status] ?? 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
  const icon = iconMap[status] ?? <Shield size={12} />
  const label = status.replace(/([A-Z])/g, ' $1').trim()

  return (
    <span className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${color}`}>
      {icon}
      {label}
    </span>
  )
}

function ModeBadge() {
  const mode = useCurrentMode()
  return (
    <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300">
      <Clock size={12} />
      {mode}
    </span>
  )
}

function ConnectionBadge() {
  const status = useConnectionStatus()
  const map = {
    connected: { label: 'Live', icon: <Wifi size={12} />, color: 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300' },
    polling: { label: 'Polling', icon: <RefreshCw size={12} />, color: 'bg-amber-100 text-amber-700 dark:bg-amber-900 dark:text-amber-300' },
    reconnecting: { label: 'Reconnecting', icon: <WifiOff size={12} />, color: 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400' },
  }
  const { label, icon, color } = map[status]
  return (
    <span className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${color}`}>
      {icon}
      {label}
    </span>
  )
}

function ConnectorBadge({ deviceId, label }: { deviceId: string; label: string }) {
  const attr = useDeviceStore((s) => s.devices[deviceId]?.attributes['switch'])
  const isOn = attr === 'on'
  const [execute] = useCommand()

  if (attr === undefined) return null

  const toggle = () => {
    const next = isOn ? 'off' : 'on'
    execute({ deviceId, command: next, optimisticAttribute: 'switch', optimisticValue: next })
  }

  return (
    <button
      onClick={toggle}
      aria-pressed={isOn}
      aria-label={`${label}: ${isOn ? 'on' : 'off'}`}
      className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium transition-colors cursor-pointer ${
        isOn
          ? 'bg-amber-100 text-amber-700 dark:bg-amber-900 dark:text-amber-300'
          : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
      }`}
    >
      {label}
    </button>
  )
}

export function SystemBar() {
  const setSidebarOpen = useDeviceStore((s) => s.setSidebarOpen)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [logModal, setLogModal] = useState<'ring' | 'hubitat' | null>(null)

  return (
    <>
      <header className="h-12 flex items-center gap-3 px-4 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shrink-0">
        <button
          className="sm:hidden p-1 rounded text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 mr-2"
          onClick={() => setSidebarOpen(true)}
          aria-label="Open menu"
        >
          <Menu size={18} />
        </button>
        <HsmBadge />
        <ModeBadge />
        <ConnectionBadge />
        <div className="flex items-center gap-1.5 overflow-x-auto">
          <ConnectorBadge deviceId="486" label="Alarms" />
          <ConnectorBadge deviceId="905" label="Silent" />
          <ConnectorBadge deviceId="1227" label="High Alert" />
          <ConnectorBadge deviceId="1268" label="Traveling" />
          <ConnectorBadge deviceId="1327" label="PTO" />
          <ConnectorBadge deviceId="1316" label="Holiday" />
        </div>
        <div className="ml-auto flex items-center gap-2">
          <button
            onClick={() => setLogModal('ring')}
            className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
          >
            Ring Log
          </button>
          <button
            onClick={() => setLogModal('hubitat')}
            className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
          >
            Hub Log
          </button>
          <Link to="/group/system" className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors">
            System
          </Link>
          <button
            onClick={() => setSettingsOpen(true)}
            aria-label="Open settings"
            className="flex items-center text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
          >
            <Settings size={14} />
          </button>
        </div>
      </header>
      {settingsOpen && <SettingsModal onClose={() => setSettingsOpen(false)} />}
      {logModal !== null && (
        <LogViewerModal logType={logModal} onClose={() => setLogModal(null)} />
      )}
    </>
  )
}
