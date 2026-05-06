import type { FastifyInstance } from 'fastify';
import bcrypt from 'bcryptjs';
const { compare } = bcrypt;
import { config } from './config.js';
import { getDevice, setAllDevices, getAllDevices, getCachedHubVars } from './cache.js';
import type { DeviceState } from './types.js';

function makerUrl(path: string): string {
  return `http://${config.hubIP}/apps/api/${config.makerAppId}${path}?access_token=${config.accessToken}`;
}

async function verifyPin(pin: unknown): Promise<boolean> {
  if (typeof pin !== 'string' || !pin) return false;
  return compare(pin, config.pinHash);
}

export async function proxyRoutes(fastify: FastifyInstance): Promise<void> {

  // GET /api/devices
  fastify.get('/api/devices', async (_req, reply) => {
    const res = await fetch(makerUrl('/devices/all'));
    if (!res.ok) {
      return reply.status(res.status).send({ error: 'Maker API error' });
    }
    const devices = await res.json() as DeviceState[];
    setAllDevices(devices);
    return reply.send(devices);
  });

  // GET /api/devices/:id
  fastify.get<{ Params: { id: string } }>('/api/devices/:id', async (req, reply) => {
    const cached = getDevice(req.params.id);
    if (cached) return reply.send(cached);
    const res = await fetch(makerUrl(`/devices/${req.params.id}/details`));
    if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
    return reply.send(await res.json());
  });

  // PUT /api/devices/:id/:command
  fastify.put<{ Params: { id: string; command: string } }>(
    '/api/devices/:id/:command',
    async (req, reply) => {
      const res = await fetch(makerUrl(`/devices/${req.params.id}/${req.params.command}`));
      if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
      return reply.send({ ok: true });
    }
  );

  // PUT /api/devices/:id/:command/:value
  fastify.put<{ Params: { id: string; command: string; value: string } }>(
    '/api/devices/:id/:command/:value',
    async (req, reply) => {
      const res = await fetch(makerUrl(`/devices/${req.params.id}/${req.params.command}/${req.params.value}`));
      if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
      return reply.send({ ok: true });
    }
  );

  // GET /api/hsm
  fastify.get('/api/hsm', async (_req, reply) => {
    const res = await fetch(makerUrl('/hsm'));
    if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
    return reply.send(await res.json());
  });

  // PUT /api/hsm/:armMode
  fastify.put<{ Params: { armMode: string }; Body: { pin?: string } }>(
    '/api/hsm/:armMode',
    async (req, reply) => {
      if (!await verifyPin((req.body as Record<string, unknown>)?.pin)) {
        return reply.status(403).send({ error: 'Invalid PIN' });
      }
      const res = await fetch(makerUrl(`/hsm/${req.params.armMode}`));
      if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
      return reply.send({ ok: true });
    }
  );

  // GET /api/modes
  fastify.get('/api/modes', async (_req, reply) => {
    const res = await fetch(makerUrl('/modes'));
    if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
    return reply.send(await res.json());
  });

  // PUT /api/modes/:id
  fastify.put<{ Params: { id: string } }>(
    '/api/modes/:id',
    async (req, reply) => {
      const res = await fetch(makerUrl(`/modes/${req.params.id}`));
      if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
      return reply.send({ ok: true });
    }
  );

  // GET /api/hubvariables
  fastify.get('/api/hubvariables', async (_req, reply) => {
    const res = await fetch(makerUrl('/hubvariables'));
    if (!res.ok) {
      // Maker API failed — return whatever we have cached from webhook pushes
      const cached = getCachedHubVars();
      const asArray = Object.entries(cached).map(([name, value]) => ({ name, value, type: 'string' }));
      return reply.send(asArray);
    }
    const raw = await res.json() as unknown[];
    // Merge in any hub vars received via webhook that aren't in the Maker API response
    const cached = getCachedHubVars();
    const makerNames = new Set(
      Array.isArray(raw) ? (raw as Record<string, unknown>[]).map((v) => v.name as string) : []
    );
    const extra = Object.entries(cached)
      .filter(([name]) => !makerNames.has(name))
      .map(([name, value]) => ({ name, value, type: 'string' }));
    return reply.send(Array.isArray(raw) ? [...raw, ...extra] : extra);
  });

  // PUT /api/hubvariables/:name
  fastify.put<{ Params: { name: string }; Body: { value: unknown } }>(
    '/api/hubvariables/:name',
    async (req, reply) => {
      const body = req.body as Record<string, unknown>;
      const encodedValue = encodeURIComponent(String(body.value ?? ''));
      const res = await fetch(
        makerUrl(`/hubvariables/${req.params.name}/${encodedValue}`)
      );
      if (!res.ok) return reply.status(res.status).send({ error: 'Maker API error' });
      return reply.send({ ok: true });
    }
  );

  // POST /api/verify-pin
  fastify.post<{ Body: { pin?: string } }>(
    '/api/verify-pin',
    async (req, reply) => {
      if (!await verifyPin((req.body as Record<string, unknown>)?.pin)) {
        return reply.status(403).send({ error: 'Invalid PIN' });
      }
      return reply.send({ ok: true });
    }
  );
}
