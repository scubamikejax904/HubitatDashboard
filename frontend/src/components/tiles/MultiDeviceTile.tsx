import { useState, useContext } from 'react'
import { Plus, Minus, X, ChevronDown } from 'lucide-react'
import { useGroupStore } from '../../store/groupStore'
import { useDeviceStore, useDeviceAttribute, useIsPending } from '../../store/deviceStore'
import { useCommand } from '../../hooks/useCommand'
import { EditModeContext } from '../../context/EditModeContext'
import { autoTileType, TILE_TYPE_LABELS, availableTileTypes } from '../../utils/autoTileType'
import type { TileType } from '../../types'
import { showToast } from '../../utils/toast'

interface Props {
  tileId: string
  groupId: string
}

// ── Mini device cell renderers ──────────────────────────────────────────────

function MiniSwitch({ deviceId, label }: { deviceId: string; label: string }) {
  const state = useDeviceAttribute(deviceId, 'switch')
  const isPending = useIsPending(deviceId)
  const [execute] = useCommand()
  const isOn = state === 'on'
  const toggle = () => execute({ deviceId, command: isOn ? 'off' : 'on', optimisticAttribute: 'switch', optimisticValue: isOn ? 'off' : 'on' })
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <button
        onClick={toggle}
        disabled={isPending}
        className={`px-2 py-0.5 rounded text-[10px] font-medium transition-all active:scale-95 disabled:opacity-50 ${
          isOn ? 'bg-green-500 text-white' : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
        }`}
      >
        {isOn ? 'On' : 'Off'}
      </button>
    </div>
  )
}

function MiniDimmer({ deviceId, label }: { deviceId: string; label: string }) {
  const state = useDeviceAttribute(deviceId, 'switch')
  const level = useDeviceAttribute(deviceId, 'level')
  const isPending = useIsPending(deviceId)
  const [execute] = useCommand()
  const isOn = state === 'on'
  const toggle = () => execute({ deviceId, command: isOn ? 'off' : 'on', optimisticAttribute: 'switch', optimisticValue: isOn ? 'off' : 'on' })
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <button
        onClick={toggle}
        disabled={isPending}
        className={`px-2 py-0.5 rounded text-[10px] font-medium transition-all active:scale-95 disabled:opacity-50 ${
          isOn ? 'bg-yellow-400 text-gray-900' : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
        }`}
      >
        {isOn && level !== undefined ? `${level}%` : isOn ? 'On' : 'Off'}
      </button>
    </div>
  )
}

function MiniTemperature({ deviceId, label }: { deviceId: string; label: string }) {
  const temp = useDeviceAttribute(deviceId, 'temperature')
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">
        {temp !== undefined ? `${temp}°` : '—'}
      </p>
    </div>
  )
}

function MiniContact({ deviceId, label }: { deviceId: string; label: string }) {
  const contact = useDeviceAttribute(deviceId, 'contact')
  const isOpen = contact === 'open'
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <span className={`text-[10px] font-semibold ${isOpen ? 'text-orange-500' : 'text-green-500'}`}>
        {contact !== undefined ? (isOpen ? 'Open' : 'Closed') : '—'}
      </span>
    </div>
  )
}

function MiniMotion({ deviceId, label }: { deviceId: string; label: string }) {
  const motion = useDeviceAttribute(deviceId, 'motion')
  const active = motion === 'active'
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <span className={`text-[10px] font-semibold ${active ? 'text-blue-500' : 'text-gray-400'}`}>
        {motion !== undefined ? (active ? 'Active' : 'Clear') : '—'}
      </span>
    </div>
  )
}

function MiniPresence({ deviceId, label }: { deviceId: string; label: string }) {
  const presence = useDeviceAttribute(deviceId, 'presence')
  const present = presence === 'present'
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <span className={`text-[10px] font-semibold ${present ? 'text-green-500' : 'text-gray-400'}`}>
        {presence !== undefined ? (present ? 'Home' : 'Away') : '—'}
      </span>
    </div>
  )
}

function MiniGeneric({ deviceId, label }: { deviceId: string; label: string }) {
  const state = useDeviceAttribute(deviceId, 'switch')
  return (
    <div className="flex flex-col items-center gap-0.5">
      <p className="text-[10px] text-gray-900 dark:text-white truncate w-full text-center leading-tight">{label}</p>
      <p className="text-[10px] text-gray-700 dark:text-gray-300">{state !== undefined ? String(state) : '—'}</p>
    </div>
  )
}

function MiniDeviceCell({ deviceId, tileType, label }: { deviceId: string; tileType: TileType; label: string }) {
  switch (tileType) {
    case 'switch':
    case 'connector': return <MiniSwitch deviceId={deviceId} label={label} />
    case 'dimmer':
    case 'rgbw':      return <MiniDimmer deviceId={deviceId} label={label} />
    case 'temperature': return <MiniTemperature deviceId={deviceId} label={label} />
    case 'contact':   return <MiniContact deviceId={deviceId} label={label} />
    case 'motion':    return <MiniMotion deviceId={deviceId} label={label} />
    case 'presence':  return <MiniPresence deviceId={deviceId} label={label} />
    default:          return <MiniGeneric deviceId={deviceId} label={label} />
  }
}

// ── Type selector dropdown ───────────────────────────────────────────────────

function TypeSelector({
  deviceId,
  groupId,
  currentType,
}: { deviceId: string; groupId: string; currentType: TileType }) {
  const [open, setOpen] = useState(false)
  const devices = useDeviceStore((s) => s.devices)
  const setTileTypeOverride = useGroupStore((s) => s.setTileTypeOverride)
  const device = devices[deviceId]
  const options = device ? availableTileTypes(device) : ([currentType] as TileType[])

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-0.5 text-[9px] text-blue-400 hover:text-blue-300 transition-colors"
      >
        {TILE_TYPE_LABELS[currentType] ?? currentType}
        <ChevronDown size={8} />
      </button>
      {open && (
        <div className="absolute z-30 left-0 top-full mt-0.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-lg min-w-[90px]">
          {options.map((t) => (
            <button
              key={t}
              onClick={() => { setTileTypeOverride(groupId, deviceId, t); setOpen(false) }}
              className={`block w-full text-left px-2 py-1 text-[10px] hover:bg-gray-100 dark:hover:bg-gray-700 ${t === currentType ? 'font-semibold text-blue-500' : 'text-gray-700 dark:text-gray-300'}`}
            >
              {TILE_TYPE_LABELS[t] ?? t}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Add-device mini picker ───────────────────────────────────────────────────

function MiniDevicePicker({
  tileId,
  existingDeviceIds,
  onClose,
}: { tileId: string; existingDeviceIds: Set<string>; onClose: () => void }) {
  const [query, setQuery] = useState('')
  const devices = useDeviceStore((s) => s.devices)
  const addDeviceToMultiTile = useGroupStore((s) => s.addDeviceToMultiTile)

  const filtered = Object.values(devices)
    .filter((d) => !existingDeviceIds.has(d.id))
    .filter((d) => !query || d.label.toLowerCase().includes(query.toLowerCase()))
    .sort((a, b) => a.label.localeCompare(b.label))

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bg-white dark:bg-gray-800 w-full sm:max-w-xs rounded-t-2xl sm:rounded-xl shadow-2xl flex flex-col max-h-[70vh]">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Add to Panel</h3>
          <button onClick={onClose} className="p-1 rounded text-gray-400 hover:text-gray-700 dark:hover:text-white"><X size={16} /></button>
        </div>
        <div className="px-3 py-2 border-b border-gray-100 dark:border-gray-700">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search devices…"
            autoFocus
            className="w-full px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex-1 overflow-y-auto">
          {filtered.length === 0
            ? <p className="px-4 py-3 text-sm text-gray-400 text-center">No devices available.</p>
            : filtered.map((d) => (
                <button
                  key={d.id}
                  onClick={() => { addDeviceToMultiTile(tileId, d.id); onClose() }}
                  className="w-full flex items-center gap-2 px-4 py-2 text-sm text-left text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 border-b border-gray-100 dark:border-gray-700/50 last:border-0"
                >
                  <span className="flex-1 truncate font-medium">{d.label}</span>
                  <span className="text-[10px] text-gray-400 font-mono">{autoTileType(d)}</span>
                </button>
              ))
          }
        </div>
      </div>
    </div>
  )
}

// ── Main component ───────────────────────────────────────────────────────────

export function MultiDeviceTile({ tileId, groupId }: Props) {
  const editMode = useContext(EditModeContext)
  const [showPicker, setShowPicker] = useState(false)
  const [editingLabel, setEditingLabel] = useState(false)

  const cfg = useGroupStore((s) => s.multiTileConfigs[tileId])
  const tileTypeOverrides = useGroupStore((s) => s.tileTypeOverrides[groupId] ?? {})
  const updateMultiTileConfig = useGroupStore((s) => s.updateMultiTileConfig)
  const removeDeviceFromMultiTile = useGroupStore((s) => s.removeDeviceFromMultiTile)
  const removeMultiTile = useGroupStore((s) => s.removeMultiTile)
  const devices = useDeviceStore((s) => s.devices)

  if (!cfg) return null

  const { deviceIds, cols, label = 'Panel' } = cfg
  const clampedCols = Math.max(1, Math.min(cols, 4))

  const setCols = (delta: number) => {
    const next = Math.max(1, Math.min(clampedCols + delta, 4))
    updateMultiTileConfig(tileId, { cols: next })
  }

  const existingSet = new Set(deviceIds)

  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm bg-white dark:bg-gray-800 p-2">
      {/* Header */}
      <div className="flex items-center justify-between mb-1.5">
        {editMode && editingLabel ? (
          <input
            autoFocus
            className="text-[10px] font-semibold uppercase tracking-wide bg-transparent border-b border-blue-400 text-gray-900 dark:text-white outline-none w-24"
            defaultValue={label}
            onBlur={(e) => { updateMultiTileConfig(tileId, { label: e.target.value.trim() || 'Panel' }); setEditingLabel(false) }}
            onKeyDown={(e) => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur(); if (e.key === 'Escape') setEditingLabel(false) }}
          />
        ) : (
          <p
            className={`text-[10px] font-semibold uppercase tracking-wide text-gray-900 dark:text-white${editMode ? ' cursor-text hover:opacity-70' : ''}`}
            onClick={() => editMode && setEditingLabel(true)}
            title={editMode ? 'Click to rename' : undefined}
          >
            {label}
          </p>
        )}
        {editMode && (
          <div className="flex items-center gap-1.5">
            <div className="flex items-center gap-1">
              <span className="text-[9px] text-gray-400">Cols</span>
              <button onClick={() => setCols(-1)} className="p-0.5 rounded hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 disabled:opacity-30" disabled={clampedCols <= 1}>
                <Minus size={10} />
              </button>
              <span className="text-[10px] font-mono w-3 text-center text-gray-700 dark:text-gray-300">{clampedCols}</span>
              <button onClick={() => setCols(1)} className="p-0.5 rounded hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 disabled:opacity-30" disabled={clampedCols >= 4}>
                <Plus size={10} />
              </button>
            </div>
            <button
              onClick={() => { removeMultiTile(groupId, tileId); showToast('Panel removed') }}
              className="flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] bg-red-600 hover:bg-red-500 text-white rounded font-medium"
            >
              <X size={9} /> Remove
            </button>
          </div>
        )}
      </div>

      {/* Device grid */}
      <div
        className="grid gap-1.5"
        style={{ gridTemplateColumns: `repeat(${clampedCols}, minmax(0, 1fr))` }}
      >
        {deviceIds.map((deviceId) => {
          const device = devices[deviceId]
          if (!device) return null
          const resolvedType: TileType = tileTypeOverrides[deviceId] ?? autoTileType(device)
          return (
            <div
              key={deviceId}
              className="relative flex flex-col items-center justify-center rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-200 dark:bg-gray-700 p-1.5 min-h-[44px]"
            >
              <MiniDeviceCell deviceId={deviceId} tileType={resolvedType} label={device.label} />
              {editMode && (
                <>
                  <button
                    onClick={() => removeDeviceFromMultiTile(tileId, deviceId)}
                    className="absolute -top-1 -right-1 z-10 rounded-full bg-red-500 text-white p-0.5 shadow"
                    aria-label={`Remove ${device.label}`}
                  >
                    <X size={8} />
                  </button>
                  <TypeSelector deviceId={deviceId} groupId={groupId} currentType={resolvedType} />
                </>
              )}
            </div>
          )
        })}

        {/* Add button */}
        {editMode && (
          <button
            onClick={() => setShowPicker(true)}
            className="flex items-center justify-center rounded-lg border border-dashed border-gray-400 dark:border-gray-500 bg-gray-200 dark:bg-gray-700 min-h-[44px] text-gray-500 hover:text-blue-400 hover:border-blue-400 transition-colors"
            aria-label="Add device"
          >
            <Plus size={14} />
          </button>
        )}
      </div>

      {showPicker && (
        <MiniDevicePicker
          tileId={tileId}
          existingDeviceIds={existingSet}
          onClose={() => setShowPicker(false)}
        />
      )}
    </div>
  )
}
