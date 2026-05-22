import { Sunrise, Sunset, Moon, CloudMoon } from 'lucide-react'
import { useHubVariable } from '../../store/deviceStore'

interface SunEntry {
  label: string
  hubVarName: string
  Icon: React.ElementType
  iconClass: string
}

const SUN_ENTRIES: SunEntry[] = [
  { label: 'Sunrise',    hubVarName: 'Sunrise',           Icon: Sunrise,   iconClass: 'text-yellow-400' },
  { label: 'Sunset',     hubVarName: 'Sunset',            Icon: Sunset,    iconClass: 'text-orange-400' },
  { label: 'Civil Dusk', hubVarName: 'CivilDusk',         Icon: CloudMoon, iconClass: 'text-indigo-400' },
  { label: 'Full Dark',  hubVarName: 'AstronomicalDusk',  Icon: Moon,      iconClass: 'text-blue-500'   },
]

function SunEntry({ entry }: { entry: SunEntry }) {
  const value = useHubVariable(entry.hubVarName)
  return (
    <div className="flex flex-col items-center justify-center gap-0.5 p-2">
      <entry.Icon size={18} className={entry.iconClass} />
      <p className="text-xs font-medium text-gray-900 dark:text-white">{entry.label}</p>
      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 tabular-nums">
        {value !== undefined ? String(value) : '—'}
      </p>
    </div>
  )
}

export function SunTimesTile() {
  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm bg-white dark:bg-gray-800 p-3">
      <p className="text-xs font-medium text-gray-400 dark:text-gray-500 mb-2 text-center uppercase tracking-wide">Sun Times</p>
      <div className="grid grid-cols-2 gap-1 divide-x divide-y divide-gray-100 dark:divide-gray-700">
        {SUN_ENTRIES.map((entry) => (
          <SunEntry key={entry.hubVarName} entry={entry} />
        ))}
      </div>
    </div>
  )
}
