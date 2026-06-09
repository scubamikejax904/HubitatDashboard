import Fastify from 'fastify';
import cors from '@fastify/cors';
import formbody from '@fastify/formbody';
import { config } from './config.js';
import { proxyRoutes } from './proxy.js';
import { webhookRoutes } from './webhook.js';
import { gpsRoutes } from './gpsRoutes.js';
import { clientCount } from './sse.js';
import { getAllDevices, setAllDevices } from './cache.js';

const fastify = Fastify({ logger: true });

await fastify.register(cors, { origin: true });
await fastify.register(formbody);
await fastify.register(proxyRoutes);
await fastify.register(webhookRoutes);
await fastify.register(gpsRoutes, { prefix: '/api/gps-track' });

// Health check
fastify.get('/api/health', async () => ({
  ok: true,
  clients: clientCount(),
  devices: getAllDevices().length,
  timestamp: Date.now(),
}));

// Hydrate device cache on startup
async function hydrateCache(): Promise<void> {
  try {
    const url = `http://${config.hubIP}/apps/api/${config.makerAppId}/devices/all?access_token=${config.accessToken}`;
    const res = await fetch(url);
    if (res.ok) {
      const devices = await res.json() as Array<{ id: string; label: string; type: string; attributes: Record<string, string | number | boolean | null>; commands?: string[] }>;
      setAllDevices(devices);
      fastify.log.info(`[cache] Hydrated ${devices.length} devices from Maker API`);
    } else {
      fastify.log.warn(`[cache] Maker API returned ${res.status} — cache empty until first webhook`);
    }
  } catch (err) {
    fastify.log.warn(`[cache] Could not reach Maker API on startup: ${err} — cache empty until first webhook`);
  }
}

try {
  await fastify.listen({ port: config.backendPort, host: '0.0.0.0' });
  fastify.log.info(`Hubitat Dashboard backend listening on port ${config.backendPort}`);
  fastify.log.info(`Set Maker API webhook postURL to: ${config.postUrl}`);
  await hydrateCache();
} catch (err) {
  fastify.log.error(err);
  process.exit(1);
}
