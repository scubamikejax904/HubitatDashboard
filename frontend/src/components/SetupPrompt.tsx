import { useState } from 'react'
import { Plus, Upload, CloudDownload, LayoutDashboard } from 'lucide-react'
import { useGroupStore } from '../store/groupStore'
import { CreateGroupModal } from './CreateGroupModal'

interface Props {
  onImport?: () => void
}

export function SetupPrompt({ onImport }: Props) {
  const [showCreateModal, setShowCreateModal] = useState(false)
  const groupOrder = useGroupStore((s) => s.groupOrder)
  const customGroups = useGroupStore((s) => s.customGroups)
  const addCustomGroup = useGroupStore((s) => s.addCustomGroup)

  const hasAnyGroups = groupOrder.length > 0 || customGroups.length > 0

  const handleCreate = (name: string, iconName: string) => {
    addCustomGroup({
      id: `custom-${Date.now()}`,
      displayName: name,
      iconName,
    })
    setShowCreateModal(false)
  }

  return (
    <div className="flex items-center justify-center min-h-[60vh] p-6">
      <div className="max-w-md w-full text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-100 dark:bg-blue-900/30 mb-6">
          <LayoutDashboard size={32} className="text-blue-600 dark:text-blue-400" />
        </div>

        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">
          {hasAnyGroups ? 'No groups to display' : 'Welcome to Hubitat Dashboard'}
        </h2>
        <p className="text-gray-500 dark:text-gray-400 mb-8">
          {hasAnyGroups
            ? 'All groups are empty. Create a group and add devices to get started, or import a backup.'
            : 'Get started by creating your first group, or import a configuration backup.'}
        </p>

        <div className="flex flex-col gap-3">
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center justify-center gap-2 w-full px-4 py-3 rounded-xl bg-blue-600 text-white font-medium hover:bg-blue-700 transition-colors"
          >
            <Plus size={18} />
            Create Your First Group
          </button>

          {onImport && (
            <button
              onClick={onImport}
              className="flex items-center justify-center gap-2 w-full px-4 py-3 rounded-xl bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 font-medium hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            >
              <Upload size={18} />
              Import Configuration
            </button>
          )}
        </div>

        <div className="mt-8 pt-6 border-t border-gray-200 dark:border-gray-700">
          <p className="text-xs text-gray-400 dark:text-gray-500 mb-3">
            You can also sync from your Hubitat hub:
          </p>
          <div className="flex gap-2 justify-center">
            <CloudDownload size={14} className="text-gray-400" />
            <span className="text-xs text-gray-500 dark:text-gray-400">
              Use the Hub Pull button in the sidebar to pull your config from the hub
            </span>
          </div>
        </div>
      </div>

      {showCreateModal && (
        <CreateGroupModal
          onClose={() => setShowCreateModal(false)}
          onConfirm={handleCreate}
        />
      )}
    </div>
  )
}
