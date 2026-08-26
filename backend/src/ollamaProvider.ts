/**
 * Ollama provider adapter — local model via the Ollama chat API.
 */
import { config } from './config.js';
import {
  DEFAULT_OLLAMA_BASE_URL,
  providerModel,
  providerTimeout,
  type ChatProvider,
} from './aiProviders.js';

/** Ollama is enabled unless explicitly disabled via config. Missing section = not configured. */
function enabled(): boolean {
  const o = config.ollama;
  if (!o) return false;
  return o.enabled !== false;
}

function baseUrl(): string {
  return config.ollama?.baseUrl ?? DEFAULT_OLLAMA_BASE_URL;
}

/** Strip Qwen3  thinking blocks from the response content. */
function stripThinking(text: string): string {
  // /think.../thinking/ across newlines; drop anything that isn't stripped to empty.
  const cleaned = text.replace(/[\s\S]*?<\/?think[^>]*>/gi, '').trim();
  return cleaned || text.trim();
}

async function chat(system: string, user: string): Promise<string> {
  const model = providerModel('ollama');
  const url = `${baseUrl().replace(/\/$/, '')}/api/chat`;
  let res: Response;
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model,
        stream: false,
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user },
        ],
        options: { temperature: 0.3 },
      }),
      signal: AbortSignal.timeout(providerTimeout('ollama')),
    });
  } catch (err) {
    const cause = err instanceof Error ? err.message : String(err);
    throw new Error(
      `Ollama request failed (is the Ollama service running on ${baseUrl()}?). ${cause}`,
    );
  }
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`Ollama HTTP ${res.status}: ${body.slice(0, 300)}`);
  }
  const j = (await res.json()) as { message?: { content?: string } };
  const content = j.message?.content ?? '';
  if (!content) throw new Error('Ollama returned an empty response');
  return stripThinking(content);
}

export const ollamaProvider: ChatProvider = {
  id: 'ollama',
  label: 'Local AI (Ollama)',
  enabled,
  chat,
};
