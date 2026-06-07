import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import type { Config } from './types.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function loadConfig(): Config {
  const configPath = join(__dirname, '..', 'config.json');
  let raw: unknown;
  try {
    raw = JSON.parse(readFileSync(configPath, 'utf-8'));
  } catch {
    console.error(`[config] Cannot read config.json at ${configPath}`);
    console.error('[config] Copy backend/config.json.example to backend/config.json and fill in your values.');
    process.exit(1);
  }

  const cfg = raw as Record<string, unknown>;
  const required: (keyof Config)[] = ['hubIP', 'makerAppId', 'accessToken', 'backendPort', 'pinHash', 'postUrl'];
  for (const key of required) {
    if (!cfg[key]) {
      console.error(`[config] Missing required field: "${key}" — check config.json`);
      process.exit(1);
    }
  }

  // DATABASE_URL env var takes precedence over config.json databaseUrl
  if (process.env.DATABASE_URL) {
    cfg['databaseUrl'] = process.env.DATABASE_URL;
  }

  return cfg as unknown as Config;
}

export const config: Config = loadConfig();
