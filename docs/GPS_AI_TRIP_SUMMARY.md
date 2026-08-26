# GPS AI Trip Summary

A **Report** button on the GPS Tracker page that turns the currently-filtered
track into a short, human-readable trip summary — naming businesses and
addresses, how long you stayed at each stop, and travel time between them.

```
[Filtered GPS points] → [stop detection] → [reverse geocode stops] → [AI narrates]
```

Two AI backends are supported and switchable in the UI:

| Provider | Default model | Host | Privacy |
|---|---|---|---|
| **Local AI (Ollama)** | `qwen3.8:27b` | your LAN | coordinates never leave your network |
| **Cloud (OpenRouter)** | `deepseek/deepseek-v4-flash` | openrouter.ai | sends coordinates/timestamps to OpenRouter |

---

## Configuration (`backend/config.json`)

Both blocks are optional. If **neither** is present, the Report feature is
disabled. If only one is present, no toggle shows and that provider is used.

```json
{
  "gpsMap": { "csvUrl": "https://docs.google.com/spreadsheets/d/.../export?format=csv&gid=0" },

  "home": {
    "label": "Home",
    "address": "135 Cassady Street, Umatilla, FL",
    "lat": 28.9263,
    "long": -81.6646,
    "radiusM": 250
  },

  "ollama": {
    "enabled": true,
    "baseUrl": "http://192.168.0.175:11434",
    "model": "qwen3.8:27b"
  },

  "openrouter": {
    "enabled": true,
    "model": "deepseek/deepseek-v4-flash",
    "timeoutMs": 60000,
    "apiKey": "<your key, or set the OPENROUTER_API_KEY env var instead>"
  }
}
```

### `home` (optional, recommended)

Known anchor/location. Any stop whose centroid falls within `radiusM` meters of
`lat`/`long` is labeled **"Home — 135 Cassady Street, Umatilla, FL"** and is
**excluded** from the business lookup. This stops a nearby business (e.g. an
adjacent worship center) from being wrongly attributed to your home stop.
`radiusM` defaults to 200 m if omitted.

### Env overrides

- `OLLAMA_BASE_URL` / `OLLAMA_MODEL` — override Ollama host / model tag.
- `OPENROUTER_API_KEY` — API key for the OpenRouter provider (used when
  `openrouter.apiKey` is not set in config.json).

> Set the Ollama `model` to the exact tag installed on your machine
> (`ollama list`). The default `qwen3.8:27b` matches this setup.

---

## How it works

1. **Detect stops** (server-side, deterministic): points that cluster within
   ~75 m for at least ~4 minutes are treated as one stop. Travel legs are the
   gaps between stops, with haversine mileage.
2. **Name each stop** — two lookups run in parallel and resolve the GPS centroid
   to a human label:
   - **Nearest business** via the **Overpass API** (free, no key): finds the
     closest named shop/amenity/leisure within ~150 m so a parking-lot stop
     becomes "Hobby Lobby" instead of an address. Results are cached and
     rate-limited (multiple mirrors with failover).
   - **Address** via **OpenStreetMap Nominatim** (free, no key) — used when no
     business is nearby, yielding e.g. "135 Cassady Street, Umatilla".
3. **Ask the AI** for a narrative, feeding it the structured stops/legs data
   (not the raw coordinate array). Qwen3 reasoning tags are stripped, and any
   `locationHint` ("on US-441 in Mount Dora") is woven into business phrasing.

**Note on business names:** the LLM cannot invent them from coordinates — they
are resolved by the Overpass/Nominatim layer. If the phone's parked location is
not on a mapped business, the stop falls back to a town / street label.

**Dependencies:** naming stops requires outbound internet access from the
backend to `nominatim.openstreetmap.org` and a public Overpass mirror
(`overpass-api.de` first, with fallbacks). Ollama summaries keep trip data
on-LAN; OpenRouter summaries send it to the cloud — choose in the UI.

---

## Usage

1. Open the **GPS Track** page.
2. Set the date range for the trip you want summarized (defaults to today).
3. Click **Report** (green button). If both providers are enabled, pick
   **Local AI** or **Cloud** from the dropdown beside it.
4. The summary appears in a green card below the toolbar, with the provider
   that produced it labeled. It clears automatically when you change the date
   range so you never read a stale trip.