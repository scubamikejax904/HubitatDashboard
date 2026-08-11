import type { FastifyInstance } from 'fastify';
import { fetchGpsData, isGpsConfigured } from './gpsService.js';

export async function gpsRoutes(fastify: FastifyInstance): Promise<void> {
  /**
   * GET /api/gps-track
   * Query: ?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD (both optional)
   */
  fastify.get('/', async (req, reply) => {
    if (!isGpsConfigured()) {
      return reply.status(503).send({
        message: 'GPS feature not configured. Add gpsMap section to backend/config.json.',
      });
    }

    const { startDate, endDate, tzOffset } = req.query as {
      startDate?: string;
      endDate?: string;
      tzOffset?: string;
    };

    try {
      let data = await fetchGpsData();

      // Server-side date filtering
      if (startDate || endDate) {
        const offsetMin = tzOffset !== undefined ? parseInt(tzOffset, 10) : 0;
        const offsetMs = Number.isFinite(offsetMin) ? offsetMin * 60 * 1000 : 0;

        // Parse dates as UTC, then shift by client's timezone offset
        // so the filter window matches the client's local midnight-to-midnight
        const start = startDate
          ? new Date(startDate + 'T00:00:00Z').getTime() + offsetMs
          : 0;
        const end = endDate
          ? new Date(endDate + 'T23:59:59.999Z').getTime() + offsetMs
          : 8640000000000000;

        if ((startDate && isNaN(start)) || (endDate && isNaN(end))) {
          return reply.status(400).send({
            message: 'Invalid date format. Use YYYY-MM-DD.',
          });
        }

        data = data.filter((item) => {
          const t = new Date(item.timestamp).getTime();
          return !isNaN(t) && t >= start && t <= end;
        });
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
}
