# GPS Map: Default Filter to "today" + Remove Track Lines

## Goal

Two changes to the GPS map viewer page in the HubitatDashboard web frontend:
1. The date filter should default to "today" (both start and end date set to the current date) instead of being empty (showing all data).
2. Remove the blue polyline that connects track points — only the individual point markers should remain.

## Current Context

- **Project**: HubitatDashboard monorepo (`frontend` + `backend` workspaces)
- **Frontend stack**: React 19 + TypeScript + Vite + Leaflet/React-Leaflet + Tailwind CSS v4 + Zustand
- **Target file**: `frontend/src/components/GpsMapPage.tsx` (267 lines, sole file needing changes)
- **Backend**: Fastify API at `GET /api/gps-track?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` — no backend changes needed
- **Config convention**: `.env` files at root; no changes to config needed

## Proposed Approach

Both changes are purely frontend and confined to a single file. No backend, config, or dependency changes required.

### Change 1: Default filter to "today"

The `startDate` and `endDate` state variables on lines 63-64 are initialized as `''`. Change them to default to `new Date().toISOString().split('T')[0]` (today's date in `YYYY-MM-DD` format).

This means:
- On first load, the fetch will request only today's data (the `useEffect` on line 97-99 already calls `fetchData()` on mount).
- The date input fields will visually show today's date.
- The "Clear" button (lines 180-188) still works to reset both fields to empty and re-fetch all data.

### Change 2: Remove polylines between track points

Line 234 renders a `<Polyline>` component:
```tsx
{path.length > 1 && <Polyline positions={path} pathOptions={{ color: '#3B82F6', weight: 3 }} />}
```

Remove this line entirely. The `path` variable (lines 108-111) and `sortedPoints` will still be used by `FitBounds` and the `CircleMarker` rendering, so `path` must stay — only the JSX element is removed.

The `Polyline` import from `react-leaflet` (line 2) should also be cleaned up since it will no longer be used.

## Step-by-Step Plan

1. **Edit `frontend/src/components/GpsMapPage.tsx`**:
   a. Remove `Polyline` from the import on line 2.
   b. Change `useState('')` for `startDate` (line 63) to `useState(new Date().toISOString().split('T')[0])`.
   c. Change `useState('')` for `endDate` (line 64) to `useState(new Date().toISOString().split('T')[0])`.
   d. Remove the `<Polyline>` JSX element on line 234 (the entire `{path.length > 1 && ...}` expression).

2. **Verify the build**: Run `npm run build` from the repo root to confirm TypeScript compilation succeeds.

3. **Verify no lint errors**: The existing `eslint` config should pass (the removed `Polyline` import would cause an unused-import error if left in place).

## Files Likely to Change

| File | Change |
|------|--------|
| `frontend/src/components/GpsMapPage.tsx` | Remove `Polyline` import; default dates to today; remove `<Polyline>` JSX |

## Tests / Validation

- `npm run build` from repo root (runs `tsc -b && vite build` in frontend workspace) — must pass with zero errors.
- Manual verification: load the GPS map page and confirm:
  - Date inputs show today's date on first load.
  - Only today's data points are fetched/shown.
  - No blue lines connect the markers.
  - "Clear" button resets dates and shows all data.
  - Start (green) and end (red) markers still display correctly.

## Risks, Tradeoffs, and Open Questions

- **Timezone consideration**: `new Date().toISOString().split('T')[0]` uses UTC, which could be "yesterday" or "tomorrow" depending on the user's timezone. If Tim is in a US timezone and it's evening, the UTC date could be the next day. This is a known limitation of the simple approach. If this is a concern, use a local-date helper like:
  ```ts
  const today = new Date();
  const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  ```
  The plan should use the local-date approach to avoid timezone edge cases.

- **No backend changes**: The backend already supports `startDate`/`endDate` query params, so no API changes needed.

- **No test files to update**: There are no existing unit tests for `GpsMapPage.tsx`. The Playwright test at `frontend/tests/dashboard.spec.ts` does not cover the GPS map page.

## Likely Blockers

- None anticipated. This is a straightforward single-file change.
