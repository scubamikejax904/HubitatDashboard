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

    const { startDate, endDate } = req.query as { startDate?: string; endDate?: string };

    try {
      let data = await fetchGpsData();

      // Server-side date filtering
      if (startDate || endDate) {
        const start = startDate ? new Date(startDate + 'T00:00:00Z') : new Date(0);
        const end = endDate ? new Date(endDate + 'T23:59:59.999Z') : new Date(8640000000000000);

        if (isNaN(start.getTime()) || isNaN(end.getTime())) {
          return reply.status(400).send({
            message: 'Invalid date format. Use YYYY-MM-DD.',
          });
        }

        data = data.filter((item) => {
          const t = new Date(item.timestamp).getTime();
          return !isNaN(t) && t >= start.getTime() && t <= end.getTime();
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
