export interface DeviceState {
  id: string;
  label: string;
  type: string;
  attributes: Record<string, string | number | boolean | null>;
  commands?: string[];
}

export interface SSEEvent {
  deviceId: string;
  attribute: string;
  value: string | number | boolean | null;
  timestamp: number;
}

export type TileType =
  | 'switch' | 'dimmer' | 'rgbw' | 'contact' | 'motion'
  | 'temperature' | 'power-meter' | 'button' | 'lock'
  | 'connector' | 'hub-variable' | 'hsm' | 'mode'
  | 'ring-detection' | 'presence' | 'battery' | 'sun-times' | 'multi-device';

export interface TileConfig {
  deviceId?: string;
  label: string;
  tileType: TileType;
  hubVarName?: string;
  wide?: boolean;
  multiTileId?: string;
  /** User-editable display title override (per-group). Undefined/blank = use label (device name). */
  title?: string;
}

export interface MultiTileConfig {
  deviceIds: string[];
  cols: number;
  label?: string;
  /** Per-device display title overrides within the panel, keyed by deviceId. */
  labels?: Record<string, string>;
}

export interface GroupConfig {
  id: string;
  displayName: string;
  icon: string;
  tiles: TileConfig[];
}
