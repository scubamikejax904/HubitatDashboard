import { useState, useEffect, useMemo, useCallback } from 'react'
import { MapContainer, TileLayer, Polyline, CircleMarker, Popup, useMap } from 'react-leaflet'
import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Filter, Loader2 } from 'lucide-react'

interface GpsDataPoint {
  timestamp: string
  lat: number
  long: number
}

const mapStyle = {
  width: '100%',
  height: 'calc(100vh - 140px)',
  minHeight: '400px',
}

/** Fit map bounds to the data points */
function FitBounds({ points }: { points: { lat: number; lng: number }[] }) {
  const map = useMap()
  useEffect(() => {
    if (points.length > 1) {
      const bounds = L.latLngBounds(points.map((p) => [p.lat, p.lng] as [number, number]))
      map.fitBounds(bounds, { padding: [40, 40] })
    } else if (points.length === 1) {
      map.setView([points[0].lat, points[0].lng], 14)
    }
  }, [points, map])
  return null
}

export function GpsMapPage() {
  const navigate = useNavigate()
  const [configured, setConfigured] = useState(true)
  const [data, setData] = useState<GpsDataPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  // Check if the backend has the GPS feature configured
  useEffect(() => {
    fetch('/api/gps-track/config')
      .then((r) => r.json())
      .then((cfg: { configured: boolean }) => setConfigured(cfg.configured))
      .catch(() => setConfigured(false))
  }, [])

  const fetchData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams()
      if (startDate) params.set('startDate', startDate)
      if (endDate) params.set('endDate', endDate)
      const url = `/api/gps-track?${params.toString()}`
      const res = await fetch(url)
      if (!res.ok) {
        const body = await res.json().catch(() => ({ message: `HTTP ${res.status}` }))
        throw new Error((body as { message?: string }).message ?? `HTTP ${res.status}`)
      }
      const json = (await res.json()) as { data: GpsDataPoint[] }
      setData(json.data ?? [])
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [startDate, endDate])

  useEffect(() => {
    fetchData()
  }, [])

  // Sort chronologically
  const sortedPoints = useMemo(
    () => [...data].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()),
    [data],
  )

  // Polyline path
  const path = useMemo(
    () => sortedPoints.map((p) => ({ lat: p.lat, lng: p.long })) as { lat: number; lng: number }[],
    [sortedPoints],
  )

  // Center map
  const center = useMemo((): [number, number] => {
    if (sortedPoints.length === 0) return [39.8283, -98.5795] // US center
    const mid = sortedPoints[Math.floor(sortedPoints.length / 2)]
    return [mid.lat, mid.long]
  }, [sortedPoints])

  // Not configured
  if (!configured) {
    return (
      <div className="flex flex-col items-center justify-center h-full p-6 text-gray-500 dark:text-gray-400">
        <p className="text-lg mb-2">GPS Track not configured</p>
        <p className="text-sm">
          Add a <code className="bg-gray-200 dark:bg-gray-700 px-1 rounded">gpsMap</code> section to{' '}
          <code className="bg-gray-200 dark:bg-gray-700 px-1 rounded">backend/config.json</code>
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      {/* Top bar */}
      <div className="flex items-center gap-4 px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 flex-wrap">
        <button
          onClick={() => navigate('/')}
          className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-300 transition-colors"
        >
          <ArrowLeft size={16} />
          <span>Dashboard</span>
        </button>

        <div className="h-5 w-px bg-gray-300 dark:bg-gray-600" />

        <span className="text-sm font-medium text-gray-700 dark:text-gray-200">GPS Track</span>

        <div className="flex items-center gap-2 ml-auto flex-wrap">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200"
          />
          <span className="text-gray-400 text-sm">to</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200"
          />
          <button
            onClick={fetchData}
            disabled={loading}
            className="flex items-center gap-1 px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {loading ? <Loader2 size={14} className="animate-spin" /> : <Filter size={14} />}
            <span>Filter</span>
          </button>
          <button
            onClick={() => {
              setStartDate('')
              setEndDate('')
            }}
            className="px-2 py-1 text-sm text-gray-500 hover:text-gray-300 transition-colors"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Status bar */}
      {!loading && (
        <div className="px-4 py-1.5 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-850 border-b border-gray-200 dark:border-gray-700">
          {sortedPoints.length} data point{sortedPoints.length !== 1 ? 's' : ''}
          {sortedPoints.length > 0 && (
            <>
              {' '}
              from {new Date(sortedPoints[0].timestamp).toLocaleString()} to{' '}
              {new Date(sortedPoints[sortedPoints.length - 1].timestamp).toLocaleString()}
            </>
          )}
        </div>
      )}

      {/* Error state */}
      {error && (
        <div className="mx-4 mt-2 p-3 rounded bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
          {error}
        </div>
      )}

      {/* Map */}
      <div className="flex-1 p-4">
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 size={32} className="animate-spin text-blue-500" />
          </div>
        ) : (
          <MapContainer
            center={center}
            zoom={sortedPoints.length > 0 ? 14 : 4}
            maxZoom={20}
            style={mapStyle}
            scrollWheelZoom
          >
            <TileLayer
              attribution='&copy; Google'
              url="https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"
              maxZoom={20}
            />
            <FitBounds points={path} />
            {path.length > 1 && <Polyline positions={path} pathOptions={{ color: '#3B82F6', weight: 3 }} />}
            {sortedPoints.map((point, idx) => {
              const isStart = idx === 0
              const isEnd = idx === sortedPoints.length - 1
              const color = isStart ? '#22C55E' : isEnd ? '#EF4444' : '#3B82F6'
              return (
                <CircleMarker
                  key={idx}
                  center={[point.lat, point.long]}
                  radius={isStart || isEnd ? 7 : 4}
                  pathOptions={{ color: '#fff', weight: 1.5, fillColor: color, fillOpacity: 0.9 }}
                >
                  <Popup>
                    <div className="text-xs">
                      <div className="font-medium">{new Date(point.timestamp).toLocaleString()}</div>
                      <div className="text-gray-500 mt-0.5">
                        {point.lat.toFixed(6)}, {point.long.toFixed(6)}
                      </div>
                      {(isStart || isEnd) && (
                        <div className="mt-0.5 font-semibold text-gray-700">
                          {isStart ? '🟢 Start' : '🔴 End'}
                        </div>
                      )}
                    </div>
                  </Popup>
                </CircleMarker>
              )
            })}
          </MapContainer>
        )}
      </div>
    </div>
  )
}
