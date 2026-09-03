/**
 * AI "Explain" routes.
 *
 * Two POST handlers that feed structured dashboard data to the existing chatWith()
 * AI layer and return a short plain-English story:
 *
 *   POST /ai/explain-events  -> narrate a window of raw hub/hubitat events
 *   POST /ai/explain-alarm   -> explain why the homeowner got an alarm alert
 *
 * Both follow the same convention: chatWith() is the only AI entry point, errors
 * come back as HTTP 200 { summary: null, provider: null, error: string } so the
 * caller can render a reader-friendly failure without branching on status codes.
 */
import type { FastifyInstance } from 'fastify';
import {
  chatWith,
  getProviderLabel,
  isAnyProviderEnabled,
} from './aiProviders.js';

/** Raw hub event shape as emitted by the hub log (mirrors LogViewerModal). */
export interface HubEvent {
  ts?: string;
  pkg?: string;
  title?: string;
  text?: string;
}

interface ExplainEventsBody {
  events?: HubEvent[];
  provider?: string;
  /** Max events to consider (most recent first); bounds token usage. */
  count?: number;
  /**
   * Client timezone offset in minutes as returned by Date.getTimezoneOffset()
   * (repo convention — GPS date filters use the same). Local wall time = UTC − offset.
   */
  tzOffset?: number;
}

interface ExplainAlarmBody {
  alarm?: {
    armed?: string;
    triggered?: string;
    silent?: boolean;
    source?: string;
  };
  context?: string;
  provider?: string;
}

const EVENTS_SYSTEM_PROMPT =
  'You are a home-automation engineer narrating a short window of recent Hubitat hub ' +
  'events to the homeowner in plain English. Write a 3 to 5 sentence story describing what ' +
  'happened, in chronological order, as if speaking to a non-technical person. Use only the ' +
  'event text and timestamps you are given; do not invent events, devices, or details that are ' +
  'not present. Do not mention API/package internals. If there is too little data to tell a ' +
  'meaningful story, say so plainly. Plain text, no markdown, no bullets, no headings.';

const ALARM_SYSTEM_PROMPT =
  'You are a home-automation engineer. Given the current alarm state, explain in 3-5 ' +
  'plain-English lines WHY the homeowner just received an alert (what state change or stuck ' +
  'condition this reflects and what to check). Use only the data given. If the data is ' +
  'insufficient, say so.';

/** Shared failure body so every route responds with a consistent shape. */
function errorReply(message: string) {
  return { summary: null, provider: null, error: message };
}

/**
 * Convert a raw UTC ISO timestamp to the client's local wall-clock time.
 * Local = UTC − offsetMin (Date.getTimezoneOffset() minutes). The shifted time
 * is emitted back in ISO so the rendered clock digits read as local wall time.
 * Unparseable/absent timestamps are passed through unchanged.
 */
function toLocalTime(ts: string | undefined, offsetMin: number): string | undefined {
  if (!ts) return ts;
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  return new Date(d.getTime() - offsetMin * 60 * 1000)
    .toISOString()
    .replace(/\.\d{3}Z$/, 'Z');
}

/** Run the AI call, mapping chatWith() exceptions onto the standard error shape. */
async function runChat(
  fastify: FastifyInstance,
  logTag: string,
  providerId: string,
  systemPrompt: string,
  userPayload: string,
) {
  if (!isAnyProviderEnabled()) {
    return errorReply(
      'No AI provider enabled. Enable "ollama" or "openrouter" in backend/config.json ' +
        '(openrouter also needs OPENROUTER_API_KEY set in the environment).',
    );
  }
  try {
    const summary = await chatWith(providerId, systemPrompt, userPayload);
    return {
      summary,
      provider: providerId,
      providerLabel: getProviderLabel(providerId) ?? providerId,
    };
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    fastify.log.error({ err }, logTag);
    return errorReply(msg);
  }
}

export async function aiExplainRoutes(fastify: FastifyInstance): Promise<void> {
  /**
   * POST /ai/explain-events
   * Body: { events: HubEvent[], provider?: 'ollama'|'openrouter', count?: number }
   * Returns a 3-5 sentence plain-English story of the most recent events.
   */
  fastify.post('/explain-events', async (req) => {
    const body = (req.body ?? {}) as ExplainEventsBody;
    const events = Array.isArray(body.events) ? body.events : [];
    if (events.length === 0) {
      return errorReply('No events provided to explain.');
    }

    const providerId = body.provider ?? 'ollama';
    const count = Math.min(
      Math.max(Number.isFinite(body.count) ? (body.count as number) : 50, 1),
      200,
    );
    // Client supplies its own UA timezone offset (minutes), matching the GPS filter convention.
    const tzOffsetMin = Number.isFinite(body.tzOffset) ? (body.tzOffset as number) : 0;

    // Bound payload size: keep the most recent `count` events, timestamps in client-local wall time.
    const window = events.slice(-count).map((e) => ({
      ts: toLocalTime(e.ts, tzOffsetMin),
      pkg: e.pkg,
      title: e.title,
      text: e.text,
    }));

    const userPayload = JSON.stringify({ events: window });
    return runChat(fastify, 'AI explain events failed', providerId, EVENTS_SYSTEM_PROMPT, userPayload);
  });

  /**
   * POST /ai/explain-alarm
   * Body: { alarm: { armed?, triggered?, silent?, source? }, context?: string }
   * Returns { summary: string|null, provider: string|null } — why the alert fired.
   */
  fastify.post('/explain-alarm', async (req) => {
    const body = (req.body ?? {}) as ExplainAlarmBody;
    const providerId = body.provider ?? 'ollama';
    const userPayload = JSON.stringify({
      alarm: body.alarm ?? {},
      context: body.context ?? '',
    });
    return runChat(fastify, 'AI explain alarm failed', providerId, ALARM_SYSTEM_PROMPT, userPayload);
  });
}