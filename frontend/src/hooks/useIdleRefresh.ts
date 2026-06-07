import { useEffect, useRef } from 'react'
import { useSettingsStore } from '../store/settingsStore'
import { syncDashboardState } from '../utils/loadDashboardState'
import type { SettingsStore } from '../store/settingsStore'

const ACTIVITY_EVENTS: (keyof DocumentEventMap)[] = [
  'mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click',
]

export function useIdleRefresh(): void {
  const idleRefreshMinutes = useSettingsStore((s: SettingsStore) => s.idleRefreshMinutes)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (!idleRefreshMinutes || idleRefreshMinutes <= 0) return

    const delayMs = idleRefreshMinutes * 60 * 1000

    function resetTimer() {
      if (timerRef.current) clearTimeout(timerRef.current)
      timerRef.current = setTimeout(() => {
        void syncDashboardState()
      }, delayMs)
    }

    resetTimer()
    ACTIVITY_EVENTS.forEach((e) => document.addEventListener(e, resetTimer, { passive: true }))

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
      ACTIVITY_EVENTS.forEach((e) => document.removeEventListener(e, resetTimer))
    }
  }, [idleRefreshMinutes])
}
