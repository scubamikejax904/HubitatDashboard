import { useEffect, useMemo, useState } from 'react'
import { Loader2, X } from 'lucide-react'

interface LogViewerModalProps {
  logType: 'ring' | 'hubitat'
  onClose: () => void
}

interface RingLogEntry {
  ts?: string
  text?: string
  success?: boolean
  code?: number
  err?: string | null
}

interface HubitatLogEntry {
  ts?: string
  pkg?: string
  title?: string
  text?: string
}

function extractTime(ts?: string) {
  if (!ts) return '--:--:--'

  const match = ts.match(/\b\d{2}:\d{2}:\d{2}\b/)
  if (match) return match[0]

  const parsed = new Date(ts)
  if (Number.isNaN(parsed.getTime())) return ts

  return parsed.toLocaleTimeString([], { hour12: false })
}

function isBlankSkip(err?: string | null) {
  return typeof err === 'string' && err.toLowerCase().includes('blank')
}

function RingStatusBadge({ entry }: { entry: RingLogEntry }) {
  if (entry.success) {
    return (
      <span className="inline-flex items-center rounded-full bg-green-100 px-2 py-0.5 text-[11px] font-medium text-green-700 dark:bg-green-900 dark:text-green-300">
        ✓ HTTP {entry.code ?? 200}
      </span>
    )
  }

  if (isBlankSkip(entry.err)) {
    return (
      <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-medium text-gray-600 dark:bg-gray-700 dark:text-gray-300">
        — skipped
      </span>
    )
  }

  return (
    <span className="inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-[11px] font-medium text-red-700 dark:bg-red-900 dark:text-red-300">
      ✗ {entry.err ?? 'error'}
    </span>
  )
}

export function LogViewerModal({ logType, onClose }: LogViewerModalProps) {
  const [entries, setEntries] = useState<RingLogEntry[] | HubitatLogEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    const load = async () => {
      setLoading(true)
      setError(null)

      try {
        const response = await fetch('/api/hub-file/' + (logType === 'ring' ? 'ring-log.json' : 'hubitat-log.json'), {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }

        const text = await response.text()
        let parsed: unknown

        try {
          parsed = JSON.parse(text)
        } catch {
          throw new Error('Invalid log format')
        }

        if (!Array.isArray(parsed)) {
          throw new Error('Invalid log format')
        }

        setEntries(parsed as RingLogEntry[] | HubitatLogEntry[])
      } catch (err) {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : 'Failed to load log')
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    load()

    return () => controller.abort()
  }, [logType])

  const title = useMemo(() => logType === 'ring' ? 'Ring Log' : 'Hubitat Log', [logType])

  const ringEntries = entries as RingLogEntry[]
  const hubitatEntries = entries as HubitatLogEntry[]

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="flex max-h-[70vh] w-[480px] flex-col rounded-xl bg-white shadow-xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4 dark:border-gray-700">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-200">{title}</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
            aria-label="Close log viewer"
          >
            <X size={16} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <div className="flex h-full min-h-40 items-center justify-center">
              <Loader2 size={22} className="animate-spin text-gray-400" />
            </div>
          ) : error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300">
              <p className="font-medium">{error}</p>
              <p className="mt-1 text-xs text-red-600 dark:text-red-400">Make sure the Android app is running and has uploaded a log.</p>
            </div>
          ) : entries.length === 0 ? (
            <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-900/40 dark:text-gray-400">
              No log entries found.
            </div>
          ) : logType === 'ring' ? (
            <div className="space-y-2">
              {ringEntries.map((entry, index) => (
                <div key={`${entry.ts ?? 'ring'}-${index}`} className="rounded-lg bg-gray-50 p-3 text-sm dark:bg-gray-900/40">
                  <div className="mb-1 flex items-start justify-between gap-3">
                    <span className="font-mono text-xs text-gray-500 dark:text-gray-400">{extractTime(entry.ts)}</span>
                    <RingStatusBadge entry={entry} />
                  </div>
                  <p className="text-sm text-gray-800 dark:text-gray-100">{entry.text || 'No notification text'}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="space-y-2">
              {hubitatEntries.map((entry, index) => (
                <div key={`${entry.ts ?? 'hubitat'}-${index}`} className="rounded-lg bg-gray-50 p-3 text-sm dark:bg-gray-900/40">
                  <div className="mb-1 flex items-start justify-between gap-3">
                    <span className="font-mono text-xs text-gray-500 dark:text-gray-400">{extractTime(entry.ts)}</span>
                    {entry.title ? <span className="text-xs font-medium text-gray-700 dark:text-gray-200 text-right">{entry.title}</span> : null}
                  </div>
                  <p className="text-sm text-gray-800 dark:text-gray-100">{entry.text || 'No notification text'}</p>
                  <p className="mt-1 font-mono text-[11px] text-gray-500 dark:text-gray-400">{entry.pkg || 'unknown package'}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
