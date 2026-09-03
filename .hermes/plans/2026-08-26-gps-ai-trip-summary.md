# AI Trip Summary for GPS Tracker View

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add a "Report" button to the web GPS tracker view that sends the currently-filtered track to local Ollama (default `qwen3:27b`, configurable) and renders a human-readable trip summary naming businesses/addresses, stop durations, and travel durations.

**Architecture:** New optional `ollama` config section in `backend/config.json`. New backend route `POST /api/gps-track/summary` that (1) re-fetches and filters points using the SAME date/tz logic as `/api/gps-track`, (2) detects stops (clusters of points where the device was stationary ≥ threshold), (3) reverse-geocodes each stop centroid plus trip endpoints via OpenStreetMap Nominatim (free, no key), (4) sends a structured JSON payload + prompt to the Ollama chat API, and (5) returns the summary text. Frontend adds a Report button to `GpsMapPage.tsx` toolbar that calls the endpoint with the current filter params and shows the result in a collapsible panel. Web-only — no shared-JSON/Android changes required.

**Tech Stack:** Fastify plugin module (backend), plain `fetch` to Ollama + Nominatim, React state only in `GpsMapPage.tsx` (no new stores).

---

## Design Decisions

1. **Reverse geocoding is mandatory, not optional.** An LLM cannot map `(28.8577, -81.5432)` → "Hobby Lobby". Nominatim returns `name`/`amenity`/`shop`/`house_number`/`road`/`city`/`state` from which we build strings like `"Hobby Lobby on US-441 in Eustis, FL"`. Rate limit: Nominatim public policy allows ~1 req/sec — we have ≤ ~10 stops/day, so sequential fetches with 1.1s delay are fine and cache results in-memory keyed by rounded lat/long.
2. **Stop detection is deterministic code, not LLM work.** A stop = consecutive points within 75 m of each other spanning ≥ 4 minutes. Centroid = mean of cluster coords. Travel legs = gaps between stops; leg duration = time between last fix of stop A and first fix of stop B. The LLM only narrates what we computed — it never does arithmetic on timestamps (small models are unreliable at it).
3. **Payload sent to Ollama stays small.** Send: trip date range, list of stops `{place, address, arrival, departure, durationMin}`, travel legs `{from, to, durationMin, approxMiles}`, total distance (haversine sum, computed server-side). Do NOT send raw point arrays (hundreds of coords waste context).
4. **Config shape** (all fields optional so existing installs don't break):
   ```json
   "ollama": {
     "enabled": true,
     "baseUrl": "http://localhost:11434",
     "model": "qwen3:27b"
   },
   "openrouter": {
     "enabled": true,
     "model": "deepseek/deepseek-v4-flash",
     "timeoutMs": 60000
   }
   ```
   Env overrides: `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `OPENROUTER_API_KEY` (required for openrouter; read via existing Hermes-style env sourcing — never stored in config.json).
5. **Provider abstraction.** One internal interface:
   ```ts
   interface ChatProvider {
     id: 'ollama' | 'openrouter';
     chat(systemPrompt: string, userPayload: string): Promise<string>;
   }
   ```
   Two adapters (`ollamaProvider.ts`, `openrouterProvider.ts`). Both return plain text; each adapter owns its own quirks (Qwen3 `<think>...</think>` stripping lives in the ollama adapter only; openrouter adapter reads `choices[0].message.content`). Route accepts a `?provider=` query param validated against enabled providers; default = `ollama`.
6. **UI toggle.** When ≥2 providers are enabled, GpsMapPage shows a small segmented control (`Local AI | Cloud`) next to the Report button; selection is passed as `provider=` and remembered in component state (no persistence needed — YAGNI).
7. **Frontend shows progress states**: idle → loading ("Analyzing trip…") → summary text / error. Summary panel is collapsible and cleared when the date filter changes (stale summaries are confusing).
8. **Privacy note:** choosing the openrouter provider sends trip coordinates/timestamps to OpenRouter + DeepSeek. Ollama keeps everything on-LAN. The UI labels make this visible ("Cloud").

---

### Task 1: Extend config types + loader for optional `ollama` section

**Objective:** Backend knows about the ollama config without breaking installs lacking it.

**Files:**
- Modify: `backend/src/types.ts` (add `OllamaConfig` interface + optional `ollama?: OllamaConfig` on `Config`)
- Modify: `backend/src/config.ts` (apply env overrides `OLLAMA_BASE_URL`/`OLLAMA_MODEL`; export defaults helper)

**Step 1:** In `types.ts`:
```ts
export interface OllamaConfig {
  baseUrl: string;      // default 'http://localhost:11434'
  model: string;        // default 'qwen3:27b'
  timeoutMs?: number;   // default 120000
  minStopMinutes?: number; // default 4
  stopRadiusMeters?: number; // default 75
}
```
Add `ollama?: OllamaConfig;` to the `Config` interface (with `?` — see monorepo-web-layers.md convention).

**Step 2:** In `config.ts`, after existing parsing:
```ts
if (process.env.OLLAMA_BASE_URL) cfg.ollama = { ...cfg.ollama, baseUrl: process.env.OLLAMA_BASE_URL };
if (process.env.OLLAMA_MODEL) cfg.ollama = { ...cfg.ollama, model: process.env.OLLAMA_MODEL };
```

**Step 3:** Update `backend/config.json.example` with an example `ollama` block (commented intent: omit section = feature disabled).

**Step 4:** Verify: `cd backend && npx tsc --noEmit`. Commit: `feat(gps): add optional ollama config section`.

---

### Task 2: Stop-detection + haversine utilities

**Objective:** Pure functions turning a point array into stops and legs.

**Files:**
- Create: `backend/src/gpsAnalysis.ts`
- Create: `backend/test/gpsAnalysis.test.ts` (or nearest existing test convention; if no test infra exists, use `node --experimental-strip-types` script check instead — match whatever the repo already does)

**Step 1:** Implement:
```ts
export interface GpsPoint { timestamp: string; lat: number; long: number }
export interface Stop { lat: number; long: number; arrival: string; departure: string; durationMin: number }
export interface Leg { durationMin: number; miles: number }

export function haversineMiles(a: [number, number], b: [number, number]): number
export function detectStops(points: GpsPoint[], opts: { minStopMinutes: number; radiusMeters: number }): Stop[]
export function summarizeTrip(points: GpsPoint[], opts): { stops: Stop[]; legs: Leg[]; totalMiles: number }
```
Algorithm for `detectStops`: iterate chronologically; grow a cluster while next point is within `radiusMeters` of current cluster centroid; when cluster breaks, if span ≥ `minStopMinutes`, emit stop with centroid + arrival/departure. Legs pair consecutive stops (duration = gap between departure[i] and arrival[i+1]); leading segment before first stop and trailing after last are also legs anchored to trip start/end.

**Step 2:** Test cases: single-point input → no stops, one leg; stationary cluster (same coords, 30-min span) → one stop; drive-stop-drive fixture → correct durations/mileage. Timestamps as ISO UTC strings.

**Step 3:** Run tests, expect pass. Commit: `feat(gps): stop detection + distance utils`.

---

### Task 3: Nominatim reverse-geocoder with cache + rate limiting

**Objective:** Turn stop coordinates into human place labels without hammering OSM.

**Files:**
- Create: `backend/src/reverseGeocode.ts`

**Step 1:** Implement:
```ts
const cache = new Map<string, GeocodeResult>(); // key: lat,long rounded to 4 decimals (~11 m)
export async function reverseGeocode(lat: number, long: number): Promise<GeocodeResult | null>
```
- GET `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=..&lon=..&zoom=18&addressdetails=1` with `User-Agent: hubitat-dashboard-local` header (Nominatim requires it).
- Build label: prefer `name` field (business POI name); else `${house_number} ${road}`; append `, ${city}, ${state}`. This yields both `"Hobby Lobby"` and street-address forms naturally.
- Sequential callers sleep 1100 ms between distinct uncached lookups (simple module-level promise chain); cached keys return instantly.
- Return null on any failure — summarizer degrades gracefully to raw coords.

**Step 2:** Manual verification: `curl` the Nominatim URL once for a known Eustis FL coordinate; confirm JSON fields used above exist.

**Step 3:** Commit: `feat(gps): nominatim reverse geocoder with memory cache`.

---

### Task 4: Chat provider abstraction (Ollama + OpenRouter adapters)

**Objective:** One `chat()` interface, two backends, selectable per request.

**Files:**
- Create: `backend/src/aiProviders.ts` (interface + registry + `getProvider(id)`)
- Create: `backend/src/ollamaProvider.ts`
- Create: `backend/src/openrouterProvider.ts`

**Step 1:** `aiProviders.ts`:
```ts
export type ProviderId = 'ollama' | 'openrouter';
export interface ChatProvider {
  id: ProviderId;
  label: string;              // 'Local AI (Ollama)' / 'Cloud (DeepSeek via OpenRouter)'
  enabled(): boolean;
  chat(systemPrompt: string, userPayload: string): Promise<string>;
}
export function listEnabledProviders(): { id: ProviderId; label: string }[]
export async function chatWith(providerId: string, system: string, user: string): Promise<string>
```
`chatWith` throws a descriptive error for unknown/disabled providers. `ollama.enabled === false` or missing OPENROUTER_API_KEY ⇒ that provider is not enabled.

**Step 2:** `ollamaProvider.ts`: POST `${baseUrl}/api/chat`, body `{ model, stream: false, messages, options: { temperature: 0.3 } }`. `AbortSignal.timeout(timeoutMs ?? 120000)`. Strip `<think>...</think>` from the response content (Qwen3 reasoning). Non-200 → error including body snippet (connection-refused to stopped Ollama service must produce a clear message).

**Step 3:** `openrouterProvider.ts`: POST `https://openrouter.ai/api/v1/chat/completions`, headers `{ Authorization: Bearer <key from process.env.OPENROUTER_API_KEY>, 'HTTP-Referer': 'http://localhost', X-Title: 'HubitatDashboard' }` (OpenRouter attributes/ranks apps by these), body `{ model: config.model ?? 'deepseek/deepseek-v4-flash', messages, temperature: 0.3 }`. Response read: `data.choices[0].message.content`. Timeout default 60s (cloud latency differs from local). Missing key ⇒ clear config error at call time.

**Step 4:** Smoke-test each adapter with a tiny "say hello" prompt: ollama against the running local service (`ollama list` first to confirm exact installed tag), openrouter with a real key in env. Confirm plain text out of both.

**Step 5:** Commit: `feat(gps): pluggable AI chat providers (ollama + openrouter)`.

---

### Task 5: `POST /api/gps-track/summary` route

**Objective:** Wire everything together behind one endpoint.

**Files:**
- Modify: `backend/src/gpsRoutes.ts` (add second route inside existing `gpsRoutes` plugin)
- Modify: `backend/src/server.ts` only if a new plugin file is preferred over extending gpsRoutes — recommend extending gpsRoutes since it shares filtering logic.

**Step 1:** Extract the date-filter block from the GET handler into a shared `filterPoints(query)` helper (DRY), then add:
```ts
fastify.post('/summary', async (req, reply) => { ... })
```
Query/body params: `startDate`, `endDate`, `tzOffset` (same semantics as GET).

**Step 2:** Flow: configured checks (gps configured; ≥1 AI provider enabled — 503 with a specific message naming which config is missing otherwise) → fetchGpsData → filterPoints → guard: ≥2 points else 400 "No points in range" → `summarizeTrip` → reverse-geocode stop centroids + trip start/end sequentially → build system prompt:

> You are a concise trip narrator. Given structured GPS trip data (stops with place names, addresses, durations; travel legs with durations and distances), write a short human-readable summary paragraph. For businesses use their name with a location hint, e.g. "Hobby Lobby on US-441 in Eustis". For non-business locations use the street address. Note how long was spent at each stop and travel time between them. Plain text, no markdown headers, under 200 words.

User message: `JSON.stringify({dateRange, origin, stops, legs, totalMiles})` → `chatWith(req.provider, system, user)` where `provider` comes from the query/body (`?provider=ollama|openrouter`, default `ollama`, validated against enabled providers). Return `{ summary }`.

**Also add:** `GET /api/gps-track/ai-providers` returning `{ providers: listEnabledProviders() }` so the frontend can render the toggle only for what's actually enabled.

**Step 3:** Also return intermediate data (`{ summary, stops, legs, totalMiles }`) so the frontend could show a details toggle later (cheap now, YAGNI-compliant since it's the same object).

**Step 4:** Manual test with dev server running:
```bash
curl -X POST 'http://localhost:3001/api/gps-track/summary?startDate=<recent-day>&endDate=<recent-day>&tzOffset=240'
```
Expected: JSON with a coherent English summary mentioning real place names. If Ollama isn't running, expected: clear 503 error message.

**Step 5:** Commit: `feat(gps): AI trip summary endpoint`.

---

### Task 6: Frontend Report button + summary panel

**Objective:** UI affordance on the tracker view.

**Files:**
- Modify: `frontend/src/components/GpsMapPage.tsx`

**Step 1:** Add state: `summary`, `summaryLoading`, `summaryError`. Add a "Report" button (lucide icon e.g. `FileText` or `Sparkles`) in the toolbar row alongside the existing date inputs, disabled while `loading || summaryLoading || data.length < 2`.

**Step 2:** Handler mirrors `fetchData()`'s param building (startDate, endDate, tzOffset) but POSTs to `/api/gps-track/summary`, appending `&provider=<selected>` when a provider is chosen. On mount, fetch `/api/gps-track/ai-providers`; if ≥2 providers returned, render a small segmented control next to the Report button (`Local AI | Cloud`) bound to component state (default: first = ollama); if only one, no toggle. Render below the map controls: a card with heading "Trip Summary" (+ which provider produced it), the text, and a dismiss X. While loading show spinner + "Asking <provider label> to summarize…".

**Step 3:** Clear summary/summaryError whenever `startDate`/`endDate` change (stale report guard) — add to the existing filter onChange paths.

**Step 4:** Typecheck + manual run: `cd frontend && npx tsc --noEmit`, then `npm run dev` at monorepo root, open `/gps-map`, click Report with today's data.

**Step 5:** Commit: `feat(gps): trip summary button and panel on tracker view`.

---

### Task 7: End-to-end validation + config doc

**Objective:** Prove the whole loop with both real providers.

**Steps:**
1. Confirm Ollama reachable and note actual installed tag: `ollama list` (plan defaults to `qwen3:27b` — set exactly what's installed in `config.json`). Confirm `OPENROUTER_API_KEY` present in env for the backend process.
2. Pick a day with known trips (e.g., a Hobby Lobby errand day) in the sheet; load `/gps-map`, verify stops detected on the summary's returned `stops` array make sense against the map markers.
3. Run the SAME trip through BOTH providers (toggle each way). Verify summaries mention: correct business names w/ location hints, addresses for non-business stops, stop durations matching reality, travel durations. Compare quality — if DeepSeek Flash consistently beats local Qwen, that's just informative; the toggle exists precisely for this.
4. Negative tests: Ollama stopped → selecting Local gives friendly error while Cloud still works; invalid/missing OPENROUTER_API_KEY → Cloud reports config error; empty day → 400; no providers enabled → button surfaces the 503 message text.
5. Update `docs/` (repo keeps docs in `docs/`) with a short "AI Trip Summary" section: both provider config blocks, `OPENROUTER_API_KEY` env requirement, model override env vars, dependency on internet access for Nominatim, and privacy note about cloud provider receiving coordinates.
6. Final commit: `docs: AI trip summary configuration`.

---

## Pitfalls carried in from project history

- **Timezone filter bug class**: reuse the exact `tzOffset` shift logic already in gpsRoutes.ts — do not write a second interpretation.
- **Markers not lines**: nothing here touches rendering, but don't add polylines while in the file (skill rule).
- **Deps go in the workspace that uses them**: all HTTP here uses built-in `fetch` — zero new npm packages needed anywhere.
- **Qwen3 `<think>` tags**: must be stripped or they'll appear verbatim in the summary panel.
- **Nominatim needs a User-Agent** or it returns 403.

## Out of scope (YAGNI)
- Android side (web-only feature per request; no shared-JSON changes).
- Streaming responses from Ollama.
- Alternative geocoder providers / self-hosted Nominatim.
