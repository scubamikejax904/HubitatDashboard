import { PrismaClient } from '@prisma/client'

// Singleton pattern — prevents multiple connections during tsx hot-reload in dev
declare global {
  // eslint-disable-next-line no-var
  var __prisma: PrismaClient | undefined
}

export const prisma: PrismaClient = globalThis.__prisma ?? new PrismaClient()
if (process.env.NODE_ENV !== 'production') globalThis.__prisma = prisma

// Eagerly connect so first request is not delayed
prisma.$connect().catch(() => { /* swallow — queries will error normally if DB is unavailable */ })
