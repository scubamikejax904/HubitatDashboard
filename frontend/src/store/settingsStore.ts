import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface SettingsStore {
  idleRefreshMinutes: number
  setIdleRefreshMinutes: (minutes: number) => void
  hubUsername: string
  hubPassword: string
  setHubUsername: (v: string) => void
  setHubPassword: (v: string) => void
}

export const useSettingsStore = create<SettingsStore>()(
  persist(
    (set) => ({
      idleRefreshMinutes: 5,
      setIdleRefreshMinutes: (minutes) => set(() => ({ idleRefreshMinutes: minutes })),
      hubUsername: '',
      hubPassword: '',
      setHubUsername: (v) => set(() => ({ hubUsername: v })),
      setHubPassword: (v) => set(() => ({ hubPassword: v })),
    }),
    { name: 'hubitat-settings' },
  ),
)
