import type { FastifyInstance } from 'fastify';
import { fetchGpsData, isGpsConfigured } from './gpsService.js';
import { summarizeTrip, type GpsPoint } from './gpsAnalysis.js';
import { reverseGeocode } from './reverseGeocode.js';
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

/** Shared date filtering (mirrors the GET handler) with timezone offset handling. */
function filterPoints(data: GpsPoint[], q: FilterQuery): GpsPoint[] {
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

const SYSTEM_PROMPT =
  'You are a concise trip narrator. Given structured GPS trip data (stops with place ' +
  'names, addresses, and durations; travel legs with durations and distances), write a ' +
  'short human-readable summary paragraph. For businesses use their name with a location ' +
  'hint, e.g. "Hobby Lobby on US-441 in Eustis". For non-business locations use the street ' +
  'address. Note how long was spent at each stop and the travel time between stops. ' +
  'Plain text, no markdown headers or bullets, under 200 words.';

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

    const { startDate, endDate, tzOffset, provider } = req.query as FilterQuery & {
      provider?: string;
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

      const opts = {
        minStopMinutes: 4,
        radiusMeters: 75,
      };
      const trip = summarizeTrip(filtered, opts);

      // Reverse-geocode stop centroids + trip endpoints.
      const geocodedStops = [];
      for (const s of trip.stops) {
        const g = await reverseGeocode(s.lat, s.long);
        geocodedStops.push({
          ...s,
          place: g?.label ?? `(${s.lat.toFixed(4)}, ${s.long.toFixed(4)})`,
        });
      }

      const userPayload = JSON.stringify({
        dateRange: { start: filtered[0].timestamp, end: filtered[filtered.length - 1].timestamp },
        stops: geocodedStops.map((s) => ({
          place: s.place,
          arrival: s.arrival,
          departure: s.departure,
          durationMin: s.durationMin,
        })),
        legs: trip.legs,
        totalMiles: trip.totalMiles,
      });

      const summary = await chatWith(providerId, SYSTEM_PROMPT, userPayload);

      return {
        summary,
        provider: providerId,
        providerLabel: getProviderLabel(providerId) ?? providerId,
        stops: geocodedStops,
        legs: trip.legs,
        totalMiles: trip.totalMiles,
      };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      fastify.log.error({ err }, 'GPS trip summary failed');
      return reply.status(500).send({ message: msg });
    }
  });
}
