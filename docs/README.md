# Hubitat Dashboard

A local web-based dashboard for Hubitat Elevation home automation. Controls switches, monitors sensors, and streams real-time device events — all grouped by the automation app that manages them.

## Features

- **Unlimited custom groups & subgroups** you define, each with its own icon
- **Real-time updates** via Server-Sent Events from Hubitat Maker API webhooks
- **Live GPS map** + AI **trip summaries** of where your phones have been
- **PIN-protected** security actions (HSM arm/disarm, lock control, mode changes)
- **Dark mode** with system preference detection
- **Responsive** — works on phone, tablet, and desktop
- **Docker** deployable on Raspberry Pi or NAS

## Prerequisites

- Node.js 20+
- Hubitat Elevation hub with Maker API app installed and enabled

## Quick Start

### 1. Install dependencies

```bash
npm install
```

### 2. Configure the backend

```bash
cp backend/config.json.example backend/config.json
```

Edit `backend/config.json`:

```json
{
  "hubIP": "192.168.1.x",
  "makerAppId": "123",
  "accessToken": "your-maker-api-token",
  "backendPort": 3001,
  "pinHash": "$2b$10$...",
  "postUrl": "http://<YOUR_SERVER_IP>:3001/api/webhook",
  "gpsMap": { "csvUrl": "https://docs.google.com/spreadsheets/d/.../export?format=csv&gid=0" }
}
```

`hubIP` / `makerAppId` / `accessToken` come from your Hubitat Maker API settings.
The other keys are optional — see `docs/USER_GUIDE.md` §3.3 for every field.

### 3. Generate a PIN hash

```bash
node -e "require('bcryptjs').hash('1234', 10).then(console.log)"
```

Paste the output as the `pinHash` value in `config.json`.

### 4. Configure Hubitat webhook

In Hubitat → Apps → Maker API → enable "Allow Access via Local Network" and set the **Post URL** to:

```
http://<YOUR_SERVER_IP>:3001/api/webhook
```

### 5. Run in development

```bash
npm run dev
```

Opens:
- Frontend: http://localhost:5173
- Backend API: http://localhost:3001

## Docker Deployment

```bash
cp backend/config.json.example backend/config.json
# Edit config.json with your hub details

docker-compose up -d
```

Dashboard available at **http://localhost** (port 80).

## Project Structure

```
├── backend/          # Fastify API server (tsx)
│   ├── src/
│   │   ├── server.ts      # Entry point
│   │   ├── proxy.ts       # Maker API proxy routes
│   │   ├── webhook.ts     # SSE + webhook handler
│   │   ├── gpsService.ts  # GPS CSV parsing + map data
│   │   └── aiProviders.ts # Ollama / OpenRouter trip summaries
│   └── config.json        # Local config (gitignored)
├── frontend/         # React + TypeScript + Vite
│   └── src/
│       ├── components/    # GroupPage, tiles/*, GpsMapPage, Sidebar
│       ├── store/         # Zustand: groupStore.ts, deviceStore.ts
│       └── App.tsx        # Routing
├── android/          # Kotlin + Compose app (bash gradlew assembleDebug)
└── docker-compose.yml
```

## Security Notes

- `backend/config.json` is gitignored — never commit your Maker API token
- PINs are stored as bcrypt hashes — the plain PIN never leaves the browser unencrypted
- Backend runs on your local network only
