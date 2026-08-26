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

export interface GpsConfig {
  // Optional override for the public CSV URL (defaults to Google Sheets export)
  csvUrl?: string;
}

/** Known home/anchor location — stops near here are labeled as Home. */
export interface HomeConfig {
  /** Display label, e.g. "Home (135 Cassady Street, Umatilla)". */
  label?: string;
  address?: string;
  lat: number;
  long: number;
  /** Stopping radius around home, meters. Default 200. */
  radiusM?: number;
}

export interface OllamaConfig {
  enabled?: boolean;
  baseUrl?: string;
  model?: string;
  timeoutMs?: number;
}

export interface OpenRouterConfig {
  enabled?: boolean;
  model?: string;
  timeoutMs?: number;
  /** Optional API key. Falls back to the OPENROUTER_API_KEY env var. */
  apiKey?: string;
}

export interface GpsDataPoint {
  timestamp: string;
  lat: number;
  long: number;
  device?: string;
}

export interface Config {
  hubIP: string;
  makerAppId: string;
  accessToken: string;
  backendPort: number;
  pinHash: string;
  postUrl: string;
  hubUsername?: string;
  hubPassword?: string;
  databaseUrl?: string;
  gpsMap?: GpsConfig;
  home?: HomeConfig;
  ollama?: OllamaConfig;
  openrouter?: OpenRouterConfig;
}
