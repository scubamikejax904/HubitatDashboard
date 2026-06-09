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

export interface GpsDataPoint {
  timestamp: string;
  lat: number;
  long: number;
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
}
