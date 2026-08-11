# GPS Map: Fix "today" filter timezone mismatch

## Problem

The "today" filter shows points from last night (e.g. June 19 8PM-midnight EDT) because:

1. **GPS timestamps are stored in UTC** (e.g. `2026-06-20T00:02:55Z`)
2. **Frontend sends local date** `2026-06-20` (correctly computed from browser local time)
3. **Backend treats it as UTC midnight**: `2026-06-20T00:00:00Z` to `2026-06-20T23:59:59.999Z`
4. For EDT (UTC-4), local "today" starts at `04:00 UTC`, not `00:00 UTC`
5. Points from June 19 8PM-midnight EDT = June 20 00:00-03:59 UTC leak through

Confirmed: 46 points from June 19 ~8PM EDT are incorrectly included when filtering for "today" June 20.

## Root Cause

`backend/src/gpsRoutes.ts` line 23-24:
```ts
const start = startDate ? new Date(startDate + 'T00:00:00Z') : new Date(0);
const end = endDate ? new Date(endDate + 'T23:59:59.999Z') : new Date(8640000000000000);
```
The `Z` suffix forces UTC interpretation of what is a local date string.

## Fix Strategy

**Change the backend to accept an optional `tzOffset` query parameter** (minutes from UTC, as reported by `Date.getTimezoneOffset()` in the browser). When provided, the backend shifts the filter window to match the client's local day boundaries in UTC space.

### Backend change (`backend/src/gpsRoutes.ts`)

- Accept optional `tzOffset` query param (number, minutes from UTC)
- When `tzOffset` is provided:
  - start = `startDate T00:00:00` **local** = UTC + tzOffset minutes
  - end = `endDate T23:59:59.999` **local** = UTC + tzOffset minutes
- When not provided, fall back to current UTC behavior (backward compatible)

Implementation:
```ts
const { startDate, endDate, tzOffset } = req.query as {
  startDate?: string;
  endDate?: string;
  tzOffset?: string;
};

if (startDate || endDate) {
  const offsetMin = tzOffset !== undefined ? parseInt(tzOffset, 10) : 0;
  const offsetMs = offsetMin * 60 * 1000;

  // Parse as local dates, then shift by timezone offset to get UTC
  const start = startDate
    ? new Date(startDate + 'T00:00:00').getTime() - offsetMs
    : 0;
  const end = endDate
    ? new Date(endDate + 'T23:59:59.999').getTime() - offsetMs
    : 8640000000000000;

  if (isNaN(start) || isNaN(end)) { /* error */ }

  data = data.filter((item) => {
    const t = new Date(item.timestamp).getTime();
    return !isNaN(t) && t >= start && t <= end;
  });
}
```

### Frontend change (`frontend/src/components/GpsMapPage.tsx`)

In `fetchData()`, add the browser's timezone offset to the request params:
```ts
const params = new URLSearchParams()
if (startDate) params.set('startDate', startDate)
if (endDate) params.set('endDate', endDate)
params.set('tzOffset', String(new Date().getTimezoneOffset()))
const url = `/api/gps-track?${params.toString()}`
```

`getTimezoneOffset()` returns minutes ahead of UTC (e.g. 240 for EDT/UTC-4), which is exactly what the backend needs.

## Files Changed

| File | Change |
|------|--------|
| `frontend/src/components/GpsMapPage.tsx` | Add `tzOffset` query param to fetchData() |
| `backend/src/gpsRoutes.ts` | Accept and apply `tzOffset` in date filter logic |

## Ambiguity / Simplification

The alternative approach — passing the start/end as full ISO timestamps from the frontend — would work but requires more frontend logic and couples the frontend to date math. The `tzOffset` approach is minimal, backward compatible (no `tzOffset` = old UTC behavior), and keeps date logic in one place (the backend).

## Verification

1. `npm run build` — must pass
2. Load the GPS map page, confirm only today's **local** data appears (no last-night points from before midnight local time)
3. Manually test with date range picker to confirm filter still works for arbitrary ranges
4. Check that clearing dates still shows all data

## Risks

- The `tzOffset` changes on DST boundaries mid-day, but since we apply it to the whole day range this is negligible (at most 1 hour of edge-case points near midnight on DST transition days).
- The backend's `parseInt` on `tzOffset` should handle the string-to-number conversion safely; invalid values will be `NaN` which we should guard against.
