/**
 * Trip analysis: stop detection + distance utilities.
 * Deterministic computation — the AI layer only narrates what these produce.
 */

export interface GpsPoint {
  timestamp: string;
  lat: number;
  long: number;
}

export interface Stop {
  lat: number;
  long: number;
  arrival: string;
  departure: string;
  durationMin: number;
}

export interface Leg {
  from: string; // ISO timestamp
  to: string;   // ISO timestamp
  durationMin: number;
  miles: number;
}

export interface TripSummaryData {
  stops: Stop[];
  legs: Leg[];
  totalMiles: number;
}

const EARTH_RADIUS_MILES = 3958.8;

/** Great-circle distance in miles between two [lat, lon] pairs. */
export function haversineMiles(a: [number, number], b: [number, number]): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(b[0] - a[0]);
  const dLon = toRad(b[1] - a[1]);
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a[0])) * Math.cos(toRad(b[0])) * Math.sin(dLon / 2) ** 2;
  return 2 * EARTH_RADIUS_MILES * Math.asin(Math.sqrt(s));
}

function centroid(points: GpsPoint[]): [number, number] {
  const lat = points.reduce((s, p) => s + p.lat, 0) / points.length;
  const lon = points.reduce((s, p) => s + p.long, 0) / points.length;
  return [lat, lon];
}

export interface StopOptions {
  minStopMinutes: number;
  radiusMeters: number;
}

const METERS_PER_MILE = 1609.344;

/**
 * Detect stops: consecutive points within `radiusMeters` of the running
 * cluster centroid, spanning >= `minStopMinutes`, become a stop.
 */
export function detectStops(points: GpsPoint[], opts: StopOptions): Stop[] {
  if (points.length < 2) return [];

  const sorted = [...points].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
  );

  const stops: Stop[] = [];
  let cluster: GpsPoint[] = [sorted[0]];

  const flushCluster = () => {
    if (cluster.length === 0) return;
    const spanMs =
      new Date(cluster[cluster.length - 1].timestamp).getTime() -
      new Date(cluster[0].timestamp).getTime();
    if (spanMs >= opts.minStopMinutes * 60_000) {
      const [lat, lon] = centroid(cluster);
      stops.push({
        lat: round6(lat),
        long: round6(lon),
        arrival: cluster[0].timestamp,
        departure: cluster[cluster.length - 1].timestamp,
        durationMin: Math.round(spanMs / 60_000),
      });
    }
    cluster = [];
  };

  for (let i = 1; i < sorted.length; i++) {
    const p = sorted[i];
    const c = centroid(cluster);
    const distMeters = haversineMiles(c, [p.lat, p.long]) * METERS_PER_MILE;
    if (distMeters <= opts.radiusMeters) {
      cluster.push(p);
    } else {
      flushCluster();
      cluster = [p];
    }
  }
  flushCluster();

  return stops;
}

/**
 * Full trip summary: stops, travel legs between them (plus lead-in and
 * tail-out legs anchored to trip start/end), and total mileage.
 */
export function summarizeTrip(points: GpsPoint[], opts: StopOptions): TripSummaryData {
  const sorted = [...points].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
  );
  const stops = detectStops(sorted, opts);

  // Anchor timestamps for legs: trip start, each stop's departure, trip end.
  const anchors: { t: string; lat: number; long: number }[] = [];
  const first = sorted[0];
  const last = sorted[sorted.length - 1];
  anchors.push({ t: first.timestamp, lat: first.lat, long: first.long });
  for (const s of stops) {
    anchors.push({ t: s.departure, lat: s.lat, long: s.long });
  }
  anchors.push({ t: last.timestamp, lat: last.lat, long: last.long });

  const legs: Leg[] = [];
  let totalMiles = 0;
  for (let i = 1; i < anchors.length; i++) {
    const a = anchors[i - 1];
    const b = anchors[i];
    const durationMin = Math.round(
      (new Date(b.t).getTime() - new Date(a.t).getTime()) / 60_000,
    );
    const miles = haversineMiles([a.lat, a.long], [b.lat, b.long]);
    totalMiles += miles;
    legs.push({
      from: a.t,
      to: b.t,
      durationMin: Math.max(0, durationMin),
      miles: Math.round(miles * 10) / 10,
    });
  }

  return { stops, legs, totalMiles: Math.round(totalMiles * 10) / 10 };
}

function round6(n: number): number {
  return Math.round(n * 1e6) / 1e6;
}
