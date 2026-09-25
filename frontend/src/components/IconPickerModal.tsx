import { useState } from 'react'
import { X } from 'lucide-react'
import { ICON_MAP, CUSTOM_ICON_NAMES } from '../utils/iconMap'

interface Props {
  currentIcon: string
  /** Icon names already used by other groups — these are disabled in the picker. */
  usedIcons?: string[]
  onClose: () => void
  onConfirm: (iconName: string) => void
}

/** Modal for changing an existing group's icon. Disables icons already used by other groups. */
export function IconPickerModal({ currentIcon, usedIcons = [], onClose, onConfirm }: Props) {
  const [selectedIcon, setSelectedIcon] = useState<string>(currentIcon)

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bg-white dark:bg-gray-800 w-full sm:max-w-sm rounded-t-2xl sm:rounded-xl shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-base font-semibold text-gray-900 dark:text-gray-100">Group Icon</h2>
          <button
            onClick={onClose}
            className="p-1 rounded text-gray-500 hover:text-gray-900 dark:hover:text-white transition-colors"
            aria-label="Close"
          >
            <X size={18} />
          </button>
        </div>

        <div className="p-5">
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

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => onConfirm(selectedIcon)}
              className="flex-1 px-4 py-2 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors"
            >
              Save
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
