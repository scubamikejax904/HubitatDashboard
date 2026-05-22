import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { groups as staticGroups } from '../config/groups'
import type { TileType, MultiTileConfig } from '../types'

/** Canonical JSON shape shared with the Android app */
export interface GroupExportPayload {
  version: number
  customGroups: CustomGroup[]
  groupAdditions: Record<string, string[]>
  groupExclusions: Record<string, string[]>
  groupOrder: string[]
  childGroupOrder: Record<string, string[]>
  /** v2: groupId → deviceId → typeString; v1 (legacy): deviceId → typeString */
  tileTypeOverrides: Record<string, Record<string, string>> | Record<string, string>
  tileOrder: Record<string, string[]>
  multiTileConfigs: Record<string, MultiTileConfig>
}

export interface CustomGroup {
  id: string
  displayName: string
  iconName: string
  /** undefined = top-level group; set = nested sub-group of that parent */
  parentId?: string
}

interface GroupStore {
  customGroups: CustomGroup[]
  /** Extra device IDs added to a group beyond its static assignment.
   *  For custom groups this is the sole source of members. */
  groupAdditions: Record<string, string[]>
  /** Device IDs explicitly removed from a static group. */
  groupExclusions: Record<string, string[]>
  /** Ordered list of top-level group IDs (static + top-level custom). */
  groupOrder: string[]
  /** Ordered sub-group IDs per parent: childGroupOrder[parentId] = [childId, ...] */
  childGroupOrder: Record<string, string[]>
  /** Per-group, per-device tile type overrides: groupId → deviceId → TileType. */
  tileTypeOverrides: Record<string, Record<string, TileType>>
  /** Per-group custom tile order — maps groupId to ordered list of tile IDs (deviceId or tileType). */
  tileOrder: Record<string, string[]>
  /** Config for multi-device panel tiles, keyed by synthetic tile ID (__multi-<ts>__). */
  multiTileConfigs: Record<string, MultiTileConfig>

  addCustomGroup: (group: CustomGroup, parentId?: string) => void
  removeCustomGroup: (id: string) => void
  addDeviceToGroup: (groupId: string, deviceId: string) => void
  /** Returns false if deviceId would have no remaining group; removal is blocked. */
  removeDeviceFromGroup: (groupId: string, deviceId: string) => boolean
  /** Removes a synthetic tile ID (e.g. __sunrise__) from groupAdditions; no membership check. */
  removeFromGroupAdditions: (groupId: string, id: string) => void
  moveGroupUp: (id: string) => void
  moveGroupDown: (id: string) => void
  moveSubGroupUp: (parentId: string, id: string) => void
  moveSubGroupDown: (parentId: string, id: string) => void
  setTileTypeOverride: (groupId: string, deviceId: string, tileType: TileType) => void
  setTileOrder: (groupId: string, orderedIds: string[]) => void
  /** Creates a new multi-device panel tile in a group. */
  addMultiTile: (groupId: string, tileId: string, config: MultiTileConfig) => void
  /** Updates cols (and/or deviceIds) for an existing multi-tile. */
  updateMultiTileConfig: (tileId: string, patch: Partial<MultiTileConfig>) => void
  /** Adds a device to a multi-tile's device list. */
  addDeviceToMultiTile: (tileId: string, deviceId: string) => void
  /** Removes a device from a multi-tile's device list. */
  removeDeviceFromMultiTile: (tileId: string, deviceId: string) => void
  /** Removes a multi-tile config and its groupAdditions entry. */
  removeMultiTile: (groupId: string, tileId: string) => void
  /** Replaces all dynamic config with imported data. Static group IDs are preserved in groupOrder. */
  importState: (data: GroupExportPayload) => void
}

const STATIC_GROUP_IDS = staticGroups.map((g) => g.id)

/** Static groups (excluding 'other') that contain this deviceId in their tiles array. */
function staticGroupsContaining(deviceId: string): string[] {
  return staticGroups
    .filter((g) => g.id !== 'other' && g.tiles.some((t) => t.deviceId === deviceId))
    .map((g) => g.id)
}

/** All groups a device currently appears in: static (minus exclusions) + additions. */
export function allGroupsForDevice(
  deviceId: string,
  state: Pick<GroupStore, 'groupAdditions' | 'groupExclusions'>,
): string[] {
  const { groupAdditions, groupExclusions } = state
  const staticIds = staticGroupsContaining(deviceId).filter(
    (gId) => !(groupExclusions[gId] ?? []).includes(deviceId),
  )
  const addedTo = Object.entries(groupAdditions)
    .filter(([, ids]) => ids.includes(deviceId))
    .map(([gId]) => gId)
  return [...new Set([...staticIds, ...addedTo])]
}

/** Expands v1 flat (deviceId → TileType) overrides to v2 per-group format. */
function expandV1Overrides(
  flatOverrides: Record<string, TileType>,
  groupAdditions: Record<string, string[]>,
  customGroups: CustomGroup[],
): Record<string, Record<string, TileType>> {
  const result: Record<string, Record<string, TileType>> = {}

  const applyToGroup = (groupId: string, deviceIds: string[]) => {
    for (const deviceId of deviceIds) {
      const tileType = flatOverrides[deviceId]
      if (!tileType) continue
      if (!result[groupId]) result[groupId] = {}
      result[groupId][deviceId] = tileType
    }
  }

  for (const group of staticGroups) {
    const staticDeviceIds = group.tiles.map((t) => t.deviceId).filter((id): id is string => !!id)
    applyToGroup(group.id, [...staticDeviceIds, ...(groupAdditions[group.id] ?? [])])
  }

  const staticGroupIds = new Set(STATIC_GROUP_IDS)
  for (const group of customGroups) {
    applyToGroup(group.id, groupAdditions[group.id] ?? [])
  }
  // Also cover any custom groups in groupAdditions not in customGroups list
  for (const groupId of Object.keys(groupAdditions)) {
    if (!staticGroupIds.has(groupId)) {
      applyToGroup(groupId, groupAdditions[groupId])
    }
  }

  return result
}

export const useGroupStore = create<GroupStore>()(
  persist(
    (set, get) => ({
      customGroups: [],
      groupAdditions: {},
      groupExclusions: {},
      groupOrder: STATIC_GROUP_IDS,
      childGroupOrder: {},
      tileTypeOverrides: {},
      tileOrder: {},
      multiTileConfigs: {},

      addCustomGroup: (group, parentId) =>
        set((s) => {
          const groupWithParent = parentId ? { ...group, parentId } : group
          if (parentId) {
            const existing = s.childGroupOrder[parentId] ?? []
            return {
              customGroups: [...s.customGroups, groupWithParent],
              childGroupOrder: { ...s.childGroupOrder, [parentId]: [...existing, group.id] },
            }
          }
          return {
            customGroups: [...s.customGroups, groupWithParent],
            groupOrder: [...s.groupOrder, group.id],
          }
        }),

      removeCustomGroup: (id) =>
        set((s) => {
          // Collect this group and all its descendants
          const toRemove = new Set<string>()
          const collect = (gid: string) => {
            toRemove.add(gid)
            const children = s.childGroupOrder[gid] ?? []
            children.forEach(collect)
          }
          collect(id)

          // Remove from parent's childGroupOrder if it's a sub-group
          const group = s.customGroups.find((g) => g.id === id)
          const updatedChildGroupOrder = { ...s.childGroupOrder }
          if (group?.parentId) {
            updatedChildGroupOrder[group.parentId] = (updatedChildGroupOrder[group.parentId] ?? [])
              .filter((cid) => cid !== id)
          }
          // Also remove all removed groups from childGroupOrder
          for (const rid of toRemove) {
            delete updatedChildGroupOrder[rid]
          }

          const updatedAdditions = { ...s.groupAdditions }
          for (const rid of toRemove) delete updatedAdditions[rid]

          return {
            customGroups: s.customGroups.filter((g) => !toRemove.has(g.id)),
            groupOrder: s.groupOrder.filter((gId) => !toRemove.has(gId)),
            groupAdditions: updatedAdditions,
            childGroupOrder: updatedChildGroupOrder,
          }
        }),

      addDeviceToGroup: (groupId, deviceId) =>
        set((s) => {
          const current = s.groupAdditions[groupId] ?? []
          if (current.includes(deviceId)) return {}
          return {
            groupAdditions: { ...s.groupAdditions, [groupId]: [...current, deviceId] },
          }
        }),

      removeDeviceFromGroup: (groupId, deviceId) => {
        const state = get()
        const currentGroups = allGroupsForDevice(deviceId, state)
        if (currentGroups.length <= 1) return false

        const isCustom = state.customGroups.some((g) => g.id === groupId)
        if (isCustom) {
          set((s) => ({
            groupAdditions: {
              ...s.groupAdditions,
              [groupId]: (s.groupAdditions[groupId] ?? []).filter((id) => id !== deviceId),
            },
          }))
        } else {
          set((s) => ({
            groupExclusions: {
              ...s.groupExclusions,
              [groupId]: [...(s.groupExclusions[groupId] ?? []), deviceId],
            },
          }))
        }
        return true
      },

      removeFromGroupAdditions: (groupId, id) =>
        set((s) => ({
          groupAdditions: {
            ...s.groupAdditions,
            [groupId]: (s.groupAdditions[groupId] ?? []).filter((sid) => sid !== id),
          },
        })),

      moveGroupUp: (id) =>
        set((s) => {
          const order = [...s.groupOrder]
          const idx = order.indexOf(id)
          if (idx <= 0) return {}
          ;[order[idx - 1], order[idx]] = [order[idx], order[idx - 1]]
          return { groupOrder: order }
        }),

      moveGroupDown: (id) =>
        set((s) => {
          const order = [...s.groupOrder]
          const idx = order.indexOf(id)
          if (idx < 0 || idx >= order.length - 1) return {}
          ;[order[idx + 1], order[idx]] = [order[idx], order[idx + 1]]
          return { groupOrder: order }
        }),

      moveSubGroupUp: (parentId, id) =>
        set((s) => {
          const order = [...(s.childGroupOrder[parentId] ?? [])]
          const idx = order.indexOf(id)
          if (idx <= 0) return {}
          ;[order[idx - 1], order[idx]] = [order[idx], order[idx - 1]]
          return { childGroupOrder: { ...s.childGroupOrder, [parentId]: order } }
        }),

      moveSubGroupDown: (parentId, id) =>
        set((s) => {
          const order = [...(s.childGroupOrder[parentId] ?? [])]
          const idx = order.indexOf(id)
          if (idx < 0 || idx >= order.length - 1) return {}
          ;[order[idx + 1], order[idx]] = [order[idx], order[idx + 1]]
          return { childGroupOrder: { ...s.childGroupOrder, [parentId]: order } }
        }),

      setTileTypeOverride: (groupId, deviceId, tileType) =>
        set((s) => ({
          tileTypeOverrides: {
            ...s.tileTypeOverrides,
            [groupId]: { ...(s.tileTypeOverrides[groupId] ?? {}), [deviceId]: tileType },
          },
        })),

      setTileOrder: (groupId, orderedIds) =>
        set((s) => ({ tileOrder: { ...s.tileOrder, [groupId]: orderedIds } })),

      addMultiTile: (groupId, tileId, config) =>
        set((s) => ({
          multiTileConfigs: { ...s.multiTileConfigs, [tileId]: config },
          groupAdditions: {
            ...s.groupAdditions,
            [groupId]: [...(s.groupAdditions[groupId] ?? []), tileId],
          },
        })),

      updateMultiTileConfig: (tileId, patch) =>
        set((s) => ({
          multiTileConfigs: {
            ...s.multiTileConfigs,
            [tileId]: { ...(s.multiTileConfigs[tileId] ?? { deviceIds: [], cols: 2 }), ...patch },
          },
        })),

      addDeviceToMultiTile: (tileId, deviceId) =>
        set((s) => {
          const cfg = s.multiTileConfigs[tileId]
          if (!cfg || cfg.deviceIds.includes(deviceId)) return {}
          return {
            multiTileConfigs: {
              ...s.multiTileConfigs,
              [tileId]: { ...cfg, deviceIds: [...cfg.deviceIds, deviceId] },
            },
          }
        }),

      removeDeviceFromMultiTile: (tileId, deviceId) =>
        set((s) => {
          const cfg = s.multiTileConfigs[tileId]
          if (!cfg) return {}
          return {
            multiTileConfigs: {
              ...s.multiTileConfigs,
              [tileId]: { ...cfg, deviceIds: cfg.deviceIds.filter((id) => id !== deviceId) },
            },
          }
        }),

      removeMultiTile: (groupId, tileId) =>
        set((s) => {
          const { [tileId]: _removed, ...rest } = s.multiTileConfigs
          return {
            multiTileConfigs: rest,
            groupAdditions: {
              ...s.groupAdditions,
              [groupId]: (s.groupAdditions[groupId] ?? []).filter((id) => id !== tileId),
            },
          }
        }),

      importState: (data) =>
        set(() => {
          // Preserve static group IDs not present in the import
          const mergedOrder = [...data.groupOrder]
          for (const id of STATIC_GROUP_IDS) {
            if (!mergedOrder.includes(id)) mergedOrder.push(id)
          }

          // Detect v1 (flat deviceId→type) vs v2 (nested groupId→deviceId→type)
          const rawOverrides = data.tileTypeOverrides as Record<string, unknown>
          const isV1 = data.version < 2 || Object.values(rawOverrides).some((v) => typeof v === 'string')
          let tileTypeOverrides: Record<string, Record<string, TileType>>
          if (isV1) {
            const flatOverrides = rawOverrides as Record<string, TileType>
            tileTypeOverrides = expandV1Overrides(flatOverrides, data.groupAdditions, data.customGroups)
          } else {
            tileTypeOverrides = rawOverrides as Record<string, Record<string, TileType>>
          }

          return {
            customGroups:      data.customGroups,
            groupAdditions:    data.groupAdditions,
            groupExclusions:   data.groupExclusions,
            groupOrder:        mergedOrder,
            childGroupOrder:   data.childGroupOrder,
            tileTypeOverrides,
            tileOrder:         data.tileOrder,
            multiTileConfigs:  (data as GroupExportPayload & { multiTileConfigs?: Record<string, MultiTileConfig> }).multiTileConfigs ?? {},
          }
        }),
    }),
    {
      name: 'hubitat-group-store',
      merge: (persisted, current) => {
        const stored = persisted as Partial<GroupStore>
        const storedOrder = stored.groupOrder ?? []
        // Add any new static group IDs not yet in the stored order
        const merged = [...storedOrder]
        for (const id of STATIC_GROUP_IDS) {
          if (!merged.includes(id)) merged.push(id)
        }

        // Migrate tileTypeOverrides from v1 flat format if needed
        const rawOverrides = stored.tileTypeOverrides as Record<string, unknown> | undefined
        let tileTypeOverrides: Record<string, Record<string, TileType>> = {}
        if (rawOverrides) {
          const isV1 = Object.values(rawOverrides).some((v) => typeof v === 'string')
          if (isV1) {
            tileTypeOverrides = expandV1Overrides(
              rawOverrides as Record<string, TileType>,
              stored.groupAdditions ?? {},
              stored.customGroups ?? [],
            )
          } else {
            tileTypeOverrides = rawOverrides as Record<string, Record<string, TileType>>
          }
        }

        return { ...current, ...stored, groupOrder: merged, tileTypeOverrides, multiTileConfigs: stored.multiTileConfigs ?? {} }
      },
    },
  ),
)
