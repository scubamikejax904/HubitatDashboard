import type { FastifyInstance } from 'fastify';
import bcrypt from 'bcryptjs';
const { compare } = bcrypt;
import { config } from './config.js';
import { getDevice, setAllDevices, getAllDevices, getCachedHubVars } from './cache.js';
import type { DeviceState } from './types.js';
import { prisma } from './db.js';

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

  // GET /api/hub-file/:filename
  fastify.get<{ Params: { filename: string } }>(
    '/api/hub-file/:filename',
    async (req, reply) => {
      const res = await fetch(`http://${config.hubIP}/local/${req.params.filename}`);
      if (!res.ok) return reply.status(res.status).send({ error: `Hub file not found: HTTP ${res.status}` });
      const text = await res.text();
      return reply.type('application/json').send(text);
    }
  );

  // PUT /api/hub-file/:filename — uploads a file to the hub's file manager
  // Credentials can be supplied via X-Hub-Username/X-Hub-Password request headers,
  // or fall back to hubUsername/hubPassword in config.json.
  // If neither is set, attempts upload without auth (works when hub security is disabled).
  fastify.put<{ Params: { filename: string } }>(
    '/api/hub-file/:filename',
    async (req, reply) => {
      const content = JSON.stringify(req.body);
      const username = (req.headers['x-hub-username'] as string | undefined) || config.hubUsername || '';
      const password = (req.headers['x-hub-password'] as string | undefined) || config.hubPassword || '';
      let cookie: string | null = null;

      if (username && password) {
        const loginBody = new URLSearchParams({
          username,
          password,
          submit: 'Login',
        });
        const loginRes = await fetch(
          `http://${config.hubIP}/login?loginRedirect=/`,
          { method: 'POST', body: loginBody, redirect: 'manual' }
        );
        const rawCookie = loginRes.headers.get('set-cookie');
        if (!rawCookie) {
          return reply.status(401).send({ error: 'Hub login failed — check hub credentials in App Settings' });
        }
        cookie = rawCookie.split(';')[0];
      }

      const form = new FormData();
      form.append(
        'uploadFile',
        new Blob([content], { type: 'application/octet-stream' }),
        req.params.filename
      );

      const headers: Record<string, string> = {};
      if (cookie) headers['Cookie'] = cookie;

      const uploadRes = await fetch(`http://${config.hubIP}/hub/fileManager/upload`, {
        method: 'POST',
        headers,
        body: form,
      });

      if (!uploadRes.ok) {
        return reply.status(uploadRes.status).send({ error: `Hub file manager error: HTTP ${uploadRes.status}` });
      }
      return reply.send({ ok: true });
    }
  );

  // GET /api/config — load dashboard config from DB
  fastify.get('/api/config', async (_req, reply) => {
    if (!config.databaseUrl) return reply.status(503).send({ error: 'Database not configured' });
    try {
      const row = await prisma.dashboardConfig.findUnique({ where: { id: 1 } });
      if (!row) return reply.status(404).send({ error: 'No config saved yet' });
      return reply.send({ config: row.config, updatedAt: row.updatedAt });
    } catch (e) {
      fastify.log.error(e, '[config] GET /api/config failed');
      return reply.status(503).send({ error: 'Database error' });
    }
  });

  // PUT /api/config — save dashboard config to DB (upserts single row)
  fastify.put('/api/config', async (req, reply) => {
    if (!config.databaseUrl) return reply.status(503).send({ error: 'Database not configured' });
    try {
      const row = await prisma.dashboardConfig.upsert({
        where:  { id: 1 },
        update: { config: req.body as object },
        create: { id: 1, config: req.body as object },
      });
      return reply.send({ ok: true, updatedAt: row.updatedAt });
    } catch (e) {
      fastify.log.error(e, '[config] PUT /api/config failed');
      return reply.status(503).send({ error: 'Database error' });
    }
  });
}
