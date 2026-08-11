# Hubitat Dashboard

Local web-based dashboard for Hubitat Elevation home automation. Node.js + Docker monorepo.

## How to Run

```bash
docker-compose up
# or
docker-compose up -d    # detached
```

Check `docker-compose.yml` for exact port mappings and service names.

## Key Gotchas

- **Docker required** — the app runs entirely in containers. Check `docker ps` if something isn't responding.
- **Monorepo structure** — multiple packages/services. Check `package.json` workspaces or the top-level README for the exact layout before making changes.
- **Hubitat network access** — the dashboard needs network access to your Hubitat hub. Check `docker-compose.yml` for network config and any env vars pointing to the hub's IP.
- **Documentation in `docs/`** — all detailed docs live in the `docs/` folder.

## Tech Stack

- Node.js
- Docker + docker-compose
- Related: HubitatWork (Groovy apps), Echo Speaks TB (Hubitat integration)