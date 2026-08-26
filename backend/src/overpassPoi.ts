/**
 * Overpass-based nearest-business lookup.
 * Nominatim reverse only returns street addresses; it does not tell you that a
 * stop in a parking lot is at "Hobby Lobby". This module finds the nearest named
 * POI (shop/amenity/leisure/tourism) to a coordinate so the AI can name real
 * businesses. Uses public Overpass mirrors with failover + in-memory cache.
 */

export interface NamedPoi {
  name: string;
  category: string;
  street?: string;
  city?: string;
  /** distance from the queried coordinate, in meters */
  distMeters: number;
}

const MIRRORS = [
  'https://overpass-api.de/api/interpreter',
  'https://overpass.kumi.systems/api/interpreter',
  'https://overpass.private.coffee/api/interpreter',
  'https://maps.mail.ru/osm/tools/overpass/api/interpreter',
];

// Cache nearest-POI per coordinate, rounded to ~11 m, like the geocoder.
const cache = new Map<string, NamedPoi | null>();

// Serialize network calls so we never hammer the free mirrors.
let chain: Promise<unknown> = Promise.resolve();
function enqueue<T>(fn: () => Promise<T>): Promise<T> {
  const run = chain.then(fn);
  chain = run.then(
    () => void 0,
    () => void 0,
  );
  return run;
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

function haversineMeters(
  a: [number, number],
  b: [number, number],
): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(b[0] - a[0]);
  const dLon = toRad(b[1] - a[1]);
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a[0])) * Math.cos(toRad(b[0])) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(s));
}

/** Build an Overpass query returning named POIs near a point. */
function buildQuery(lat: number, lon: number, radiusMeters: number): string {
  return (
    `[out:json][timeout:25];` +
    `(nwr["name"]["shop"](around:${radiusMeters},${lat},${lon});` +
    `nwr["name"]["amenity"](around:${radiusMeters},${lat},${lon});` +
    `nwr["name"]["leisure"](around:${radiusMeters},${lat},${lon});` +
    `nwr["name"]["tourism"](around:${radiusMeters},${lat},${lon});` +
    `);out center tags 60;`
  );
}

interface RawElem {
  tags?: Record<string, string>;
  center?: { lat: number; lon: number };
  lat?: number;
  lon?: number;
}

async function queryOverpass(
  lat: number,
  lon: number,
  radiusMeters: number,
): Promise<RawElem[]> {
  const query = buildQuery(lat, lon, radiusMeters);
  let lastErr: unknown = null;

  for (const mirror of MIRRORS) {
    try {
      const res = await fetch(mirror, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain', 'User-Agent': 'hubitat-dashboard-local' },
        body: query,
        signal: AbortSignal.timeout(20_000),
      });
      // De:ratelimit / overcapacity returns HTML, not JSON; json() would throw.
      const text = await res.text();
      const json = JSON.parse(text) as { elements?: RawElem[] };
      return Array.isArray(json.elements) ? json.elements : [];
    } catch (err) {
      lastErr = err;
    }
  }
  throw lastErr instanceof Error ? lastErr : new Error(String(lastErr));
}

/**
 * Find the nearest named business/amenity within `radiusMeters` of the
 * coordinate. Returns null when nothing mapped is inside the radius.
 */
export async function findNearbyPoi(
  lat: number,
  lon: number,
  radiusMeters = 150,
): Promise<NamedPoi | null> {
  const key = `${lat.toFixed(4)},${lon.toFixed(4)}`;
  if (cache.has(key)) return cache.get(key) ?? null;

  const result: NamedPoi | null = await enqueue(async () => {
    await sleep(800); // gentle spacing against free Overpass
    let elems: RawElem[];
    try {
      elems = await queryOverpass(lat, lon, radiusMeters);
    } catch {
      return null; // Overpass unavailable → degrade to address-only
    }

    let best: NamedPoi | null = null;
    for (const e of elems) {
      const tags = e.tags ?? {};
      // Prefer properly named POIs; skip unnamed/directional junk.
      const name = (tags.name ?? '').trim();
      if (!name) continue;
      const category =
        tags.shop ?? tags.amenity ?? tags.leisure ?? tags.tourism ?? '';
      const center = (e.center ?? {}) as { lat?: number; lon?: number };
      const plat = center.lat ?? e.lat;
      const plon = center.lon ?? e.lon;
      if (plat === undefined || plon === undefined) continue;
      const dist = haversineMeters([lat, lon], [plat, plon]);
      if (dist > radiusMeters) continue;
      // Favor the closest mapped feature.
      if (!best || dist < best.distMeters) {
        best = {
          name,
          category,
          street: tags['addr:street'],
          city: tags['addr:city'] ?? tags['addr:town'] ?? tags['addr:village'],
          distMeters: Math.round(dist),
        };
      }
    }
    return best;
  });

  cache.set(key, result);
  return result;
}