import {
  Thermometer, Siren, Moon, Lightbulb, DoorOpen,
  PersonStanding, Shield, AlertTriangle, Camera,
  Bell, Star, Clock, Zap, Settings, LayoutGrid,
  Home, Leaf, Flame, Droplets, Coffee, Tv, Music, Car,
  Wrench, Package, Tag, Globe, Heart, Cpu, Bookmark, Layers,
} from 'lucide-react'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const ICON_MAP: Record<string, React.ComponentType<any>> = {
  // Static group icons
  Thermometer, Siren, Moon, Lightbulb, DoorOpen,
  PersonStanding, Shield, AlertTriangle, Camera,
  Bell, Star, Clock, Zap, Settings, LayoutGrid,
  // Custom group icon palette
  Home, Leaf, Flame, Droplets, Coffee, Tv, Music, Car,
  Wrench, Package, Tag, Globe, Heart, Cpu, Bookmark, Layers,
}

/** Icons available for user-created custom groups */
export const CUSTOM_ICON_NAMES = [
  'Home', 'Leaf', 'Flame', 'Droplets', 'Coffee', 'Tv',
  'Music', 'Car', 'Wrench', 'Package', 'Tag', 'Globe',
  'Heart', 'Cpu', 'Bookmark', 'Layers',
] as const

/**
 * Returns the first icon in CUSTOM_ICON_NAMES not already used by another group,
 * so newly created groups get a unique icon by default. Falls back to the first
 * icon when the palette is exhausted (duplicates unavoidable).
 */
export function nextAvailableIcon(used: string[]): string {
  const usedSet = new Set(used)
  return CUSTOM_ICON_NAMES.find((n) => !usedSet.has(n)) ?? CUSTOM_ICON_NAMES[0]
}
