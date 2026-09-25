import { useState } from 'react'
import { X } from 'lucide-react'
import { ICON_MAP, CUSTOM_ICON_NAMES, nextAvailableIcon } from '../utils/iconMap'

interface Props {
  onClose: () => void
  onConfirm: (name: string, iconName: string) => void
  title?: string
  /** Icon names already used by other groups — these are disabled in the picker. */
  usedIcons?: string[]
}

export function CreateGroupModal({ onClose, onConfirm, title = 'New Group', usedIcons = [] }: Props) {
  const [name, setName] = useState('')
  const [selectedIcon, setSelectedIcon] = useState<string>(() => nextAvailableIcon(usedIcons))

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = name.trim()
    if (!trimmed) return
    onConfirm(trimmed, selectedIcon)
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bg-white dark:bg-gray-800 w-full sm:max-w-sm rounded-t-2xl sm:rounded-xl shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-base font-semibold text-gray-900 dark:text-gray-100">{title}</h2>
          <button
            onClick={onClose}
            className="p-1 rounded text-gray-500 hover:text-gray-900 dark:hover:text-white transition-colors"
            aria-label="Close"
          >
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-1">
              Group Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Workshop"
              autoFocus
              required
              className="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">
              Icon
            </label>
            <div className="grid grid-cols-8 gap-1">
              {CUSTOM_ICON_NAMES.map((iconName) => {
                const Icon = ICON_MAP[iconName]
                const isTaken = usedIcons.includes(iconName) && iconName !== selectedIcon
                return (
                  <button
                    key={iconName}
                    type="button"
                    title={isTaken ? `${iconName} (already used)` : iconName}
                    onClick={() => setSelectedIcon(iconName)}
                    disabled={isTaken}
                    className={`flex items-center justify-center p-2 rounded-lg transition-colors min-h-[40px] ${
                      selectedIcon === iconName
                        ? 'bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300'
                        : isTaken
                          ? 'text-gray-300 dark:text-gray-600 cursor-not-allowed'
                          : 'text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400'
                    }`}
                  >
                    {Icon && <Icon size={18} />}
                  </button>
                )
              })}
            </div>
          </div>

          <div className="flex gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!name.trim()}
              className="flex-1 px-4 py-2 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Create Group
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
