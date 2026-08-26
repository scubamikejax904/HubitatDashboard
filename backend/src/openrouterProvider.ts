/**
 * OpenRouter provider adapter — cloud (DeepSeek V4 Flash by default) via the
 * OpenAI-compatible chat completions endpoint.
 */
import { config } from './config.js';
import { providerModel, providerTimeout, type ChatProvider } from './aiProviders.js';

const OPENROUTER_URL = 'https://openrouter.ai/api/v1/chat/completions';

/** Enabled when config.openrouter.enabled === true AND an API key is available. */
function enabled(): boolean {
  const o = config.openrouter;
  if (!o || o.enabled !== true) return false;
  return Boolean(apiKey());
}

function apiKey(): string | undefined {
  return config.openrouter?.apiKey || process.env.OPENROUTER_API_KEY || undefined;
}

async function chat(system: string, user: string): Promise<string> {
  const key = apiKey();
  if (!key) {
    throw new Error(
      'OpenRouter provider selected but no API key is set. Add "apiKey" to the ' +
        'openrouter section of backend/config.json, or set OPENROUTER_API_KEY in the environment.',
    );
  }

  let res: Response;
  try {
    res = await fetch(OPENROUTER_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${key}`,
        'HTTP-Referer': 'http://localhost',
        'X-Title': 'HubitatDashboard',
      },
      body: JSON.stringify({
        model: providerModel('openrouter'),
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user },
        ],
        temperature: 0.3,
      }),
      signal: AbortSignal.timeout(providerTimeout('openrouter')),
    });
  } catch (err) {
    const cause = err instanceof Error ? err.message : String(err);
    throw new Error(`OpenRouter request failed: ${cause}`);
  }
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`OpenRouter HTTP ${res.status}: ${body.slice(0, 300)}`);
  }
  const j = (await res.json()) as { choices?: { message?: { content?: string } }[] };
  const content = j.choices?.[0]?.message?.content ?? '';
  if (!content) throw new Error('OpenRouter returned an empty response');
  return content.trim();
}

export const openrouterProvider: ChatProvider = {
  id: 'openrouter',
  label: 'Cloud (DeepSeek V4 Flash)',
  enabled,
  chat,
};
