import { Routes, Route, Navigate, useParams } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { SystemBar } from './components/SystemBar'
import { GroupPage } from './components/GroupPage'
import { GpsMapPage } from './components/GpsMapPage'
import { SetupPrompt } from './components/SetupPrompt'
import { ToastContainer } from './components/ToastContainer'
import { useSSE } from './hooks/useSSE'
import { useIdleRefresh } from './hooks/useIdleRefresh'
import { useConfigSync } from './hooks/useConfigSync'
import { useGroupStore } from './store/groupStore'

function GroupPageWrapper() {
  const { groupId } = useParams<{ groupId: string }>()
  const hasGroups = useHasGroups()
  if (!hasGroups) return <SetupPrompt />
  return <GroupPage groupId={groupId ?? 'environment'} />
}

/** Reads groupOrder from store to determine if any groups exist. */
function useHasGroups() {
  const groupOrder = useGroupStore((s) => s.groupOrder)
  return groupOrder.length > 0
}

/** Default route: redirect to first group if groups exist, otherwise show setup. */
function DefaultRoute() {
  const hasGroups = useHasGroups()
  if (!hasGroups) return <SetupPrompt />
  const firstGroup = useGroupStore.getState().groupOrder[0]
  return <Navigate to={`/group/${firstGroup}`} replace />
}

function App() {
  useSSE()
  useIdleRefresh()
  useConfigSync()

  return (
    <div className="flex h-screen bg-gray-100 dark:bg-gray-900">
      <Sidebar />
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <SystemBar />
        <main className="flex-1 overflow-y-auto">
          <Routes>
            <Route path="/" element={<DefaultRoute />} />
            <Route path="/group/:groupId" element={<GroupPageWrapper />} />
            <Route path="/gps-map" element={<GpsMapPage />} />
            <Route path="*" element={
              <div className="p-6 text-gray-500 dark:text-gray-400">Page not found</div>
            } />
          </Routes>
        </main>
      </div>
      <ToastContainer />
    </div>
  )
}

export default App
