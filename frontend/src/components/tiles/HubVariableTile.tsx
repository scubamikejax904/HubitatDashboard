import { useState } from 'react'
import { Pencil, Check, X } from 'lucide-react'
import { useHubVariable, useDeviceStore } from '../../store/deviceStore'
import { showToast } from '../../utils/toast'

interface Props { hubVarName: string; label: string }

const READ_ONLY_VARS = new Set(['Sunrise', 'Sunset', 'CivilDusk', 'AstronomicalDusk', 'WeatherReport'])

export function HubVariableTile({ hubVarName, label }: Props) {
  const value = useHubVariable(hubVarName)
  const setHubVariables = useDeviceStore((s) => s.setHubVariables)
  const hubVariables = useDeviceStore((s) => s.hubVariables)
  const isReadOnly = READ_ONLY_VARS.has(hubVarName)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')

  const startEdit = () => {
    if (isReadOnly) return
    setDraft(value !== undefined ? String(value) : '')
    setEditing(true)
  }

  const cancelEdit = () => setEditing(false)

  const confirmEdit = async () => {
    const prev = value
    // Optimistic update
    setHubVariables({ ...hubVariables, [hubVarName]: draft })
    setEditing(false)
    try {
      const res = await fetch(`/api/hubvariables/${encodeURIComponent(hubVarName)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ value: draft }),
      })
      if (!res.ok) throw new Error(`${res.status}`)
    } catch (err) {
      // Revert
      setHubVariables({ ...hubVariables, [hubVarName]: prev ?? '' })
      showToast(`Failed to update ${label}`, 'error')
    }
  }

  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-4 shadow-sm bg-white dark:bg-gray-800">
      <div className="flex items-center justify-between mb-1">
        <p className="text-xs font-medium text-gray-900 dark:text-white truncate">{label}</p>
        {!editing && !isReadOnly && (
          <button onClick={startEdit} className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-400" aria-label="Edit">
            <Pencil size={12} />
          </button>
        )}
      </div>
      {editing ? (
        <div className="flex items-center gap-1 mt-1">
          <input
            autoFocus
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') confirmEdit(); if (e.key === 'Escape') cancelEdit() }}
            className="flex-1 text-sm border border-gray-300 dark:border-gray-600 rounded px-2 py-1 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
          />
          <button onClick={confirmEdit} className="p-1 rounded bg-green-500 text-white" aria-label="Save"><Check size={12} /></button>
          <button onClick={cancelEdit} className="p-1 rounded bg-gray-200 dark:bg-gray-700 text-gray-600" aria-label="Cancel"><X size={12} /></button>
        </div>
      ) : (
        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 mt-1 whitespace-pre-line">
          {value !== undefined ? String(value) : '—'}
        </p>
      )}
    </div>
  )
}
