import { config } from './config.js';
import type { GpsDataPoint } from './types.js';

const SHEET_ID = '1EGNmf9XvinmTE5EGZUOBjaoeU1HIvBLHlz33TN0KfcU';

/**
 * Build the public CSV export URL for the Google Sheet.
 * If config provides a csvUrl override, use that; otherwise derive from sheet ID.
 */
function getCsvUrl(): string {
  const g = config.gpsMap;
  if (g?.csvUrl) return g.csvUrl;
  // Default: first sheet (gid=0). User can override via config.
  return `https://docs.google.com/spreadsheets/d/${SHEET_ID}/export?format=csv&gid=0`;
}

/**
 * Fetch GPS data from the publicly published Google Sheet CSV.
 */
export async function fetchGpsData(): Promise<GpsDataPoint[]> {
  const url = getCsvUrl();
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`CSV fetch failed: HTTP ${res.status} from ${url}`);
  }

  const csvText = await res.text();
  return parseCsv(csvText);
}

/* ── CSV parser ─────────────────────────────────────────────── */

function parseCsv(csv: string): GpsDataPoint[] {
  const lines = csv.split(/\r?\n/).filter((l) => l.trim() !== '');
  if (lines.length < 2) return [];

  const headers = parseCsvRow(lines[0]).map((h) => h.toLowerCase().trim());

  // Find column indices (flexible matching)
  const tsIdx = headers.findIndex((h) =>
    h.includes('timestamp') || h === 'time' || h === 'date' || h === 'datetime',
  );
  const latIdx = headers.findIndex((h) => h === 'lat' || h === 'latitude');
  const lngIdx = headers.findIndex((h) =>
    h === 'long' || h === 'lng' || h === 'longitude' || h === 'lon',
  );

  if (tsIdx === -1 || latIdx === -1 || lngIdx === -1) {
    throw new Error(
      `Could not find required columns (timestamp, lat, long). Headers: [${headers.join(', ')}]`,
    );
  }

  const points: GpsDataPoint[] = [];
  for (let i = 1; i < lines.length; i++) {
    const cells = parseCsvRow(lines[i]);
    if (cells.length < Math.max(tsIdx, latIdx, lngIdx) + 1) continue;

    const rawLat = parseFloat(cells[latIdx]);
    const rawLng = parseFloat(cells[lngIdx]);
    if (isNaN(rawLat) || isNaN(rawLng)) continue;

    const rawTs = cells[tsIdx].trim();
    if (!rawTs) continue;

    // Handle various timestamp formats
    let timestamp: string;
    if (/^\d{5,}$/.test(rawTs)) {
      // Sheets serial date number (e.g. 45473.5)
      const d = new Date(Math.round((parseFloat(rawTs) - 25569) * 86400 * 1000));
      timestamp = d.toISOString();
    } else {
      const d = new Date(rawTs);
      timestamp = isNaN(d.getTime()) ? rawTs : d.toISOString();
    }

    points.push({ timestamp, lat: rawLat, long: rawLng });
  }

  return points;
}

/** Parse a single CSV row respecting quoted fields */
function parseCsvRow(line: string): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"') {
        if (i + 1 < line.length && line[i + 1] === '"') {
          current += '"';
          i++; // skip escaped quote
        } else {
          inQuotes = false;
        }
      } else {
        current += ch;
      }
    } else {
      if (ch === '"') {
        inQuotes = true;
      } else if (ch === ',') {
        result.push(current);
        current = '';
      } else {
        current += ch;
      }
    }
  }
  result.push(current);
  return result;
}

/**
 * Returns whether GPS feature is configured.
 */
export function isGpsConfigured(): boolean {
  const g = config.gpsMap;
  if (!g) return false;
  // Always configured — the sheet ID is hardcoded, csvUrl is optional override
  return true;
}
