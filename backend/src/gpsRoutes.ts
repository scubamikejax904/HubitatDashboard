import type { FastifyInstance } from 'fastify';
import { config } from './config.js';
import { fetchGpsData, isGpsConfigured } from './gpsService.js';
import { summarizeTrip, type GpsPoint } from './gpsAnalysis.js';
import { reverseGeocode } from './reverseGeocode.js';
import { findNearbyPoi } from './overpassPoi.js';
import { roadLegMiles } from './osrmRoute.js';
import {
  chatWith,
  getProviderLabel,
  isAnyProviderEnabled,
  listEnabledProviders,
} from './aiProviders.js';

interface FilterQuery {
  startDate?: string;
  endDate?: string;
  tzOffset?: string;
}

/** A GPS point with an optional device id, as returned by the data source. */
type FilteredPoint = GpsPoint & { device?: string };

/** Shared date filtering (mirrors the GET handler) with timezone offset handling. */
function filterPoints(data: FilteredPoint[], q: FilterQuery): FilteredPoint[] {
  const { startDate, endDate, tzOffset } = q;
  if (!startDate && !endDate) return data;

  const offsetMin = tzOffset !== undefined ? parseInt(tzOffset, 10) : 0;
  const offsetMs = Number.isFinite(offsetMin) ? offsetMin * 60 * 1000 : 0;

  const start = startDate
    ? new Date(startDate + 'T00:00:00Z').getTime() + offsetMs
    : 0;
  const end = endDate
    ? new Date(endDate + 'T23:59:59.999Z').getTime() + offsetMs
    : 8640000000000000;

  return data.filter((item) => {
    const t = new Date(item.timestamp).getTime();
    return !isNaN(t) && t >= start && t <= end;
  });
}

/** Great-circle distance in meters between two [lat, lon] pairs. */
function haversineMeters(a: [number, number], b: [number, number]): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(b[0] - a[0]);
  const dLon = toRad(b[1] - a[1]);
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a[0])) * Math.cos(toRad(b[0])) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(s));
}

/** Distance-free: is this coordinate within the configured home radius? */
function isHome(lat: number, long: number): boolean {
  const h = config.home;
  if (!h || h.lat === undefined || h.long === undefined) return false;
  const radiusM = h.radiusM ?? 200;
  return haversineMeters([lat, long], [h.lat, h.long]) <= radiusM;
}

/** Label used for a stop that lands on home. */
function homeLabel(): string {
  const h = config.home;
  if (!h) return 'Home';
  const parts = [h.label || 'Home', h.address].filter(Boolean);
  return parts.join(' — ') || 'Home';
}

const SYSTEM_PROMPT =
  'You are a concise trip narrator. Given structured GPS trip data (stops with place ' +
  'names, addresses/location hints, and durations; travel legs with durations and ' +
  'distances), write a short human-readable summary paragraph. When a stop has a ' +
  '"locationHint" and is a business, phrase it like "Hobby Lobby on US-441 in Mount Dora". ' +
  'When a stop is not a business, use its place/address. Note how long was spent at each ' +
  'stop and the travel time between stops. Plain text, no markdown headers or bullets, ' +
  'under 200 words.';

export async function gpsRoutes(fastify: FastifyInstance): Promise<void> {
  /**
   * GET /api/gps-track
   * Query: ?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD (both optional) &tzOffset
   */
  fastify.get('/', async (req, reply) => {
    if (!isGpsConfigured()) {
      return reply.status(503).send({
        message: 'GPS feature not configured. Add gpsMap section to backend/config.json.',
      });
    }

    const q = req.query as FilterQuery;

    try {
      let data = await fetchGpsData();
      if (q.startDate || q.endDate) {
        data = filterPoints(data, q);
      }
      return { data };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      fastify.log.error({ err }, 'GPS track fetch failed');
      return reply.status(500).send({ message: msg });
    }
  });

  /**
   * GET /api/gps-track/config
   */
  fastify.get('/config', async () => {
    return { configured: isGpsConfigured() };
  });

  /**
   * GET /api/gps-track/ai-providers
   * Reports which AI providers are enabled so the frontend can render a toggle.
   */
  fastify.get('/ai-providers', async () => {
    return { providers: listEnabledProviders() };
  });

  /**
   * POST /api/gps-track/summary
   * Query/body: ?startDate=&endDate=&tzOffset=&provider=ollama|openrouter
   * Reverse-geocodes detected stops and asks the selected AI to narrate the trip.
   */
  fastify.post('/summary', async (req, reply) => {
    if (!isGpsConfigured()) {
      return reply.status(503).send({
        message: 'GPS feature not configured. Add gpsMap section to backend/config.json.',
      });
    }
    if (!isAnyProviderEnabled()) {
      return reply.status(503).send({
        message:
          'No AI provider enabled. Enable "ollama" or "openrouter" in backend/config.json ' +
          '(openrouter also needs OPENROUTER_API_KEY set in the environment).',
      });
    }

    const { startDate, endDate, tzOffset, provider, device, distance } = req.query as FilterQuery & {
      provider?: string;
      device?: string;
      distance?: string;
    };
    const providerId = provider ?? 'ollama';

    try {
      const data = await fetchGpsData();
      const filtered = filterPoints(data, { startDate, endDate, tzOffset });

      if (filtered.length < 2) {
        return reply.status(400).send({
          message: 'No GPS points in the selected range (need at least 2 points).',
        });
      }

      // Detect stops PER DEVICE. Merging devices corrupts detection (a moving
      // phone + a home-stationary phone interleave and split/merge clusters),
      // so we pick one target device — the one that traveled — and summarize it.
      const byDevice = new Map<string, FilteredPoint[]>();
      for (const p of filtered) {
        const d = p.device ?? '';
        if (!byDevice.has(d)) byDevice.set(d, []);
        byDevice.get(d)!.push(p);
      }

      let targetDev: string;
      if (device && byDevice.has(device)) {
        targetDev = device;
      } else {
        // Auto-select the device with the largest geographic span (the traveler).
        targetDev = '';
        let bestSpan = -1;
        for (const [d, pts] of byDevice) {
          const span = haversineMeters(
            [Math.min(...pts.map((p) => p.lat)), Math.min(...pts.map((p) => p.long))],
            [Math.max(...pts.map((p) => p.lat)), Math.max(...pts.map((p) => p.long))],
          );
          if (span > bestSpan) {
            bestSpan = span;
            targetDev = d;
          }
        }
        if (!targetDev) targetDev = byDevice.keys().next().value as string;
      }

      const targetPoints = byDevice.get(targetDev) ?? filtered;

      const opts = {
        minStopMinutes: 4,
        radiusMeters: 75,
      };
      const trip = summarizeTrip(targetPoints, opts);

      // Reverse-geocode stop centroids + trip endpoints, and try to name the
      // nearest business (Overpass) so parking-lot stops resolve to a real POI.
      const geocodedStops = [];
      for (const s of trip.stops) {
        // Home first — skip external lookups entirely when we know it's home.
        if (isHome(s.lat, s.long)) {
          geocodedStops.push({
            ...s,
            place: homeLabel(),
            kind: 'home',
            locationHint: undefined,
          });
          continue;
        }

        const [geo, poi] = await Promise.all([
          reverseGeocode(s.lat, s.long),
          findNearbyPoi(s.lat, s.long, 150),
        ]);

        let place: string;
        let locationHint: string | undefined;
        let kind: string | undefined;

        if (poi) {
          place = poi.category
            ? `${poi.name} (${poi.category})`
            : poi.name;
          kind = poi.category;
          // Location hint e.g. "on US-441 in Mount Dora" for business stops.
          const road = poi.street ?? geo?.road;
          const city = poi.city ?? geo?.city;
          if (road && city) locationHint = `on ${road} in ${city}`;
          else if (road) locationHint = `on ${road}`;
          else if (city) locationHint = `in ${city}`;
        } else {
          place = geo?.label ?? `(${s.lat.toFixed(4)}, ${s.long.toFixed(4)})`;
        }

        geocodedStops.push({ ...s, place, locationHint, kind });
      }

      // Convert leg mileage to real road distance when requested (falls back to
      // crow-flies on any OSRM failure). Anchors mirror summarizeTrip's order:
      // trip start -> each stop departure -> trip end.
      let finalLegs = trip.legs;
      let finalTotalMiles = trip.totalMiles;
      let distanceMode: 'crow' | 'road' = 'crow';
      if (distance === 'road') {
        const anchors: [number, number][] = [
          [targetPoints[0].lat, targetPoints[0].long],
          ...trip.stops.map((s) => [s.lat, s.long] as [number, number]),
          [targetPoints[targetPoints.length - 1].lat, targetPoints[targetPoints.length - 1].long],
        ];
        try {
          const roadMiles = await roadLegMiles(anchors);
          if (roadMiles.length === trip.legs.length) {
            finalLegs = trip.legs.map((leg, i) => ({ ...leg, miles: Math.round(roadMiles[i] * 10) / 10 }));
            finalTotalMiles = Math.round(roadMiles.reduce((a, b) => a + b, 0) * 10) / 10;
            distanceMode = 'road';
          }
        } catch (err) {
          // Keep crow-flies; log quietly.
          fastify.log.warn({ err }, 'OSRM road distance unavailable, falling back to crow-flies');
        }
      }

      const userPayload = JSON.stringify({
        dateRange: {
          start: targetPoints[0].timestamp,
          end: targetPoints[targetPoints.length - 1].timestamp,
        },
        stops: geocodedStops.map((s) => ({
          place: s.place,
          locationHint: s.locationHint,
          arrival: s.arrival,
          departure: s.departure,
          durationMin: s.durationMin,
        })),
        legs: finalLegs,
        totalMiles: finalTotalMiles,
      });

      const summary = await chatWith(providerId, SYSTEM_PROMPT, userPayload);

      return {
        summary,
        provider: providerId,
        providerLabel: getProviderLabel(providerId) ?? providerId,
        device: targetDev,
        devices: [...byDevice.keys()],
        distanceMode,
        stops: geocodedStops,
        legs: finalLegs,
        totalMiles: finalTotalMiles,
      };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      fastify.log.error({ err }, 'GPS trip summary failed');
      return reply.status(500).send({ message: msg });
    }
  });
}
