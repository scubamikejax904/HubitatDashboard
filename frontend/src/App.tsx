import { Routes, Route, Navigate, useParams } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { SystemBar } from './components/SystemBar'
import { GroupPage } from './components/GroupPage'
import { ToastContainer } from './components/ToastContainer'
import { useSSE } from './hooks/useSSE'
import { useIdleRefresh } from './hooks/useIdleRefresh'
import { useConfigSync } from './hooks/useConfigSync'

function GroupPageWrapper() {
  const { groupId } = useParams<{ groupId: string }>()
  return <GroupPage groupId={groupId ?? 'environment'} />
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
            <Route path="/" element={<Navigate to="/group/environment" replace />} />
            <Route path="/group/:groupId" element={<GroupPageWrapper />} />
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
