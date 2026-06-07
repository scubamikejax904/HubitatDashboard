import { useEffect, useRef } from 'react'
import { useGroupStore, type GroupExportPayload } from '../store/groupStore'

export type SyncStatus = 'idle' | 'saving' | 'saved' | 'error'

// Module-level status so Sidebar can read it without prop drilling
let _status: SyncStatus = 'idle'
const _listeners = new Set<(s: SyncStatus) => void>()

function setStatus(s: SyncStatus) {
  _status = s
  _listeners.forEach((fn) => fn(s))
}

export function getSyncStatus() { return _status }
export function subscribeSyncStatus(fn: (s: SyncStatus) => void) {
  _listeners.add(fn)
  return () => _listeners.delete(fn)
}

function buildPayload(): GroupExportPayload {
  const s = useGroupStore.getState()
  return {
    version: 2,
    customGroups:      s.customGroups,
    groupAdditions:    s.groupAdditions,
    groupExclusions:   s.groupExclusions,
    groupOrder:        s.groupOrder,
    childGroupOrder:   s.childGroupOrder,
    tileTypeOverrides: s.tileTypeOverrides,
    tileOrder:         s.tileOrder,
    multiTileConfigs:  s.multiTileConfigs,
    tileTitleOverrides: s.tileTitleOverrides,
  }
}

async function saveToDb(payload: GroupExportPayload): Promise<void> {
  const res = await fetch('/api/config', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: `HTTP ${res.status}` })) as { error?: string }
    throw new Error(err.error ?? `HTTP ${res.status}`)
  }
}

export function useConfigSync() {
  const importState = useGroupStore((s) => s.importState)
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  // Track whether the initial DB load has completed so we don't trigger a
  // save immediately after importing from DB
  const loaded = useRef(false)

  // On mount: load config from DB and hydrate store
  useEffect(() => {
    let cancelled = false
    fetch('/api/config')
      .then((res) => {
        if (!res.ok) return null // 404 = no config saved yet, or DB not configured — fine
        return res.json() as Promise<{ config: GroupExportPayload }>
      })
      .then((data) => {
        if (cancelled) return
        if (!data) {
          // DB is available but empty — seed it with current store state, then mark loaded
          saveToDb(buildPayload()).catch(() => {})
          loaded.current = true
          return
        }
        // Brief pause to let store settle before we mark loaded, so the
        // subscribe below doesn't fire on our own importState call
        importState(data.config)
        setTimeout(() => { if (!cancelled) loaded.current = true }, 200)
      })
      .catch(() => { /* DB unavailable — stay on localStorage */ })

    return () => { cancelled = true }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Subscribe to store changes and debounce-save to DB
  useEffect(() => {
    const unsub = useGroupStore.subscribe(() => {
      if (!loaded.current) return
      if (saveTimer.current) clearTimeout(saveTimer.current)
      setStatus('saving')
      saveTimer.current = setTimeout(() => {
        saveToDb(buildPayload())
          .then(() => setStatus('saved'))
          .catch(() => setStatus('error'))
      }, 1500)
    })
    return () => {
      unsub()
      if (saveTimer.current) clearTimeout(saveTimer.current)
    }
  }, [])
}
