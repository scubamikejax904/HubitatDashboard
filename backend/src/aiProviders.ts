/**
 * Pluggable AI chat providers for trip summaries.
 * Two backends: local Ollama and cloud OpenRouter (DeepSeek). Each adapter owns
 * its own wire format and quirks (e.g. stripping Qwen3 <think> blocks).
 */
import { config } from './config.js';
import { ollamaProvider } from './ollamaProvider.js';
import { openrouterProvider } from './openrouterProvider.js';

export type ProviderId = 'ollama' | 'openrouter';

export interface ChatProvider {
  id: ProviderId;
  label: string;
  enabled(): boolean;
  chat(systemPrompt: string, userPayload: string): Promise<string>;
}

const providers: ChatProvider[] = [ollamaProvider, openrouterProvider];

export function listEnabledProviders(): { id: ProviderId; label: string }[] {
  return providers.filter((p) => p.enabled()).map((p) => ({ id: p.id, label: p.label }));
}

export function isAnyProviderEnabled(): boolean {
  return providers.some((p) => p.enabled());
}

export function getProviderLabel(id: string): string | undefined {
  return providers.find((p) => p.id === id)?.label;
}

/** Default baseUrl for local Ollama (configurable via config or OLLAMA_BASE_URL env). */
export const DEFAULT_OLLAMA_BASE_URL = 'http://192.168.0.175:11434';

/** Resolve the configured model tag, or the default for the given provider. */
export function providerModel(id: ProviderId): string {
  if (id === 'ollama') return config.ollama?.model ?? 'qwen3.8:27b';
  return config.openrouter?.model ?? 'deepseek/deepseek-v4-flash';
}

export function providerTimeout(id: ProviderId): number {
  if (id === 'ollama') return config.ollama?.timeoutMs ?? 120_000;
  return config.openrouter?.timeoutMs ?? 60_000;
}

/**
 * Send a chat request to the named provider. Throws a descriptive Error for
 * unknown or disabled providers, or on transport/HTTP failure.
 */
export async function chatWith(
  providerId: string,
  system: string,
  user: string,
): Promise<string> {
  const provider = providers.find((p) => p.id === providerId);
  if (!provider) throw new Error(`Unknown AI provider: "${providerId}"`);
  if (!provider.enabled()) {
    throw new Error(`AI provider "${provider.id}" is not enabled (see backend/config.json)`);
  }
  return provider.chat(system, user);
}
