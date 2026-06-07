import { useDeviceStore } from '../store/deviceStore'

function parseHubVariables(raw: unknown): Record<string, string | number> {
  const vars: unknown[] = Array.isArray(raw)
    ? raw
    : Array.isArray((raw as Record<string, unknown>)?.variables)
      ? (raw as Record<string, unknown[]>).variables
      : []

  const record: Record<string, string | number> = {}
  for (const v of vars) {
    const item = v as Record<string, unknown>
    if (item.name && item.value !== undefined && item.value !== null)
      record[item.name as string] = item.value as string | number
  }
  return record
}

export async function syncDashboardState(): Promise<void> {
  try {
    const [devRes, varRes, modeRes] = await Promise.allSettled([
      fetch('/api/devices'),
      fetch('/api/hubvariables'),
      fetch('/api/modes'),
    ])

    if (devRes.status === 'fulfilled' && devRes.value.ok) {
      const devices = await devRes.value.json()
      useDeviceStore.getState().setAllDevices(devices)
    }

    if (varRes.status === 'fulfilled' && varRes.value.ok) {
      const raw = await varRes.value.json()
      const vars = parseHubVariables(raw)
      if (Object.keys(vars).length > 0)
        useDeviceStore.getState().setHubVariables(vars)
    }

    if (modeRes.status === 'fulfilled' && modeRes.value.ok) {
      const modes = await modeRes.value.json() as Array<{ id: string; name: string; active: boolean }>
      const active = modes.find((m) => m.active)
      if (active) useDeviceStore.getState().setCurrentMode(active.name)
    }
  } catch {
    // Hub unreachable — caller decides whether to surface errors.
  }
}
