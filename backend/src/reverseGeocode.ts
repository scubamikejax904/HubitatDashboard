/**
 * Nominatim reverse geocoder with an in-memory cache and ~1 req/sec rate limiting.
 * Converts a coordinate to a human place label ("Hobby Lobby",
 * "123 N Bay St, Eustis, FL"). Returns null on any failure; callers degrade
 * gracefully to raw coordinates.
 */

export interface GeocodeResult {
  label: string;
  name?: string;
  road?: string;
  houseNumber?: string;
  city?: string;
  state?: string;
}

const cache = new Map<string, GeocodeResult | null>();
const MIN_GAP_MS = 1100;

/** Serialize all network lookups through this chain so Nominatim gets ≥1.1s between calls. */
let chain: Promise<unknown> = Promise.resolve();

function enqueue<T>(fn: () => Promise<T>): Promise<T> {
  const run = chain.then(fn);
  // Keep the chain flowing regardless of this link's outcome.
  chain = run.then(
    () => void 0,
    () => void 0,
  );
  return run;
}

/**
 * Reverse-geocode a coordinate. Cache hits (keyed to ~11 m precision) return
 * instantly; uncached lookups are serialized with a ≥1.1s gap between network calls.
 */
export async function reverseGeocode(
  lat: number,
  long: number,
): Promise<GeocodeResult | null> {
  const key = `${lat.toFixed(4)},${long.toFixed(4)}`;
  if (cache.has(key)) return cache.get(key) ?? null;

  const url =
    `https://nominatim.openstreetmap.org/reverse` +
    `?format=jsonv2&lat=${encodeURIComponent(String(lat))}` +
    `&lon=${encodeURIComponent(String(long))}&zoom=18&addressdetails=1`;

  const result = await enqueue(async () => {
    await new Promise((r) => setTimeout(r, MIN_GAP_MS)); // delay even first call = polite
    try {
      const res = await fetch(url, {
        headers: { 'User-Agent': 'hubitat-dashboard-local', Accept: 'application/json' },
        signal: AbortSignal.timeout(10_000),
      });
      if (!res.ok) return null;
      const j = (await res.json()) as { name?: string; address?: Record<string, string> };
      const address = j.address ?? {};
      const name = j.name;
      const road = address.road;
      const houseNumber = address.house_number;
      const city = address.city ?? address.town ?? address.village;
      const state = address.state;

      let label = name ?? (road ? (houseNumber ? `${houseNumber} ${road}` : road) : '');
      if (city) label = label ? `${label}, ${city}` : city;
      if (state) label = label ? `${label}, ${state}` : state;
      if (!label) return null;

      return {
        label,
        name,
        road,
        houseNumber,
        city,
        state,
      } satisfies GeocodeResult;
    } catch {
      return null;
    }
  });

  cache.set(key, result ?? null);
  return result;
}