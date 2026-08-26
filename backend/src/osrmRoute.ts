/**
 * OSRM routing — real road distance between consecutive stop centroids.
 * Free public service (router.project-osrm.org), no key. Degrades to null on
 * any failure so the caller can fall back to haversine (crow-flies).
 */

const OSRM_URL = process.env.OSRM_URL ?? 'https://router.project-osrm.org/route/v1/driving';

/**
 * Compute driving miles between consecutive anchor points, in order.
 * Returns an array of length (anchors.length - 1). Throws on failure so the
 * caller can fall back to straight-line distance.
 */
export async function roadLegMiles(anchors: [number, number][]): Promise<number[]> {
  if (anchors.length < 2) return [];

  // OSRM coordinate order is lon,lat.
  const coords = anchors.map(([lat, lon]) => `${lon},${lat}`).join(';');
  const url = `${OSRM_URL}/${coords}?overview=false&alternatives=false`;

  const res = await fetch(url, {
    headers: { Accept: 'application/json', 'User-Agent': 'hubitat-dashboard-local' },
    signal: AbortSignal.timeout(15_000),
  });
  if (!res.ok) {
    throw new Error(`OSRM HTTP ${res.status}`);
  }
  const j = (await res.json()) as {
    routes?: {
      legs?: { distance?: number }[]; // meters per leg between consecutive waypoints
    }[];
  };
  const legs = j.routes?.[0]?.legs;
  if (!legs || legs.length === 0) {
    throw new Error('OSRM returned no route');
  }
  return legs.map((l) => (l.distance ?? 0) / 1609.344); // meters -> miles
}