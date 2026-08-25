import type { DeviceState, TileType } from '../types'

/**
 * Infer a tile type from what a device reports (attributes + commands).
 * Used by the "Other" group to auto-render unclaimed devices.
 */
export function autoTileType(device: DeviceState): TileType {
  const attrs = device.attributes
  const cmds = device.commands ?? []

  // Color bulb/strip — has hue attribute or setColor command
  if ('hue' in attrs || cmds.includes('setColor')) return 'rgbw'

  // Dimmable — has level + setLevel + switch
  if ('level' in attrs && cmds.includes('setLevel') && 'switch' in attrs) return 'dimmer'

  // Lock
  if ('lock' in attrs) return 'lock'

  // Contact sensor
  if ('contact' in attrs) return 'contact'

  // Motion sensor
  if ('motion' in attrs) return 'motion'

  // Presence
  if ('presence' in attrs) return 'presence'

  // Power meter only (no switch)
  if ('power' in attrs && !('switch' in attrs)) return 'power-meter'

  // Temperature only sensor (no switch)
  if ('temperature' in attrs && !('switch' in attrs)) return 'temperature'

  // Pushable button
  if (cmds.includes('push')) return 'button'

  // Default — treat as a plain switch
  return 'switch'
}

/**
 * Returns every TileType that a device is capable of rendering as.
 * Used to populate the "Change Type" picker in edit mode.
 */
export function availableTileTypes(device: DeviceState): TileType[] {
  const attrs = device.attributes
  const cmds = device.commands ?? []
  const types: TileType[] = []

  if ('hue' in attrs || cmds.includes('setColor')) types.push('rgbw')
  if ('level' in attrs && cmds.includes('setLevel') && 'switch' in attrs) types.push('dimmer')
  if ('switch' in attrs) {
    types.push('switch')
    types.push('connector')
    types.push('waterison')
  }
  if ('lock' in attrs) types.push('lock')
  if ('contact' in attrs) types.push('contact')
  if ('motion' in attrs) types.push('motion')
  if ('presence' in attrs) types.push('presence')
  if ('power' in attrs) types.push('power-meter')
  if ('temperature' in attrs) types.push('temperature')
  if ('battery' in attrs) types.push('battery')
  if (cmds.includes('push')) types.push('button')

  return types.length > 0 ? types : ['switch']
}

export const TILE_TYPE_LABELS: Record<string, string> = {
  'switch':       'Switch',
  'dimmer':       'Dimmer',
  'rgbw':         'Color Light',
  'lock':         'Lock',
  'contact':      'Contact',
  'motion':       'Motion',
  'presence':     'Presence',
  'power-meter':  'Power Meter',
  'temperature':  'Temperature',
  'button':       'Button',
  'battery':      'Battery',
  'connector':    'Connector',
  'waterison':    'Water Is On',
}
