# Dashboard refresh flash investigation plan

## Goal
Find what is causing the browser to visibly flash on the Hubitat dashboard, and determine whether the behavior can be changed to update data without a full page refresh.

## Current context / assumptions
- The app is a React/Vite dashboard in `frontend/`.
- The strongest candidate found so far is `frontend/src/hooks/useIdleRefresh.ts`, which calls `window.location.reload()` after a period of inactivity.
- The app also uses SSE and fallback polling in `frontend/src/hooks/useSSE.ts`, so most data refreshes should not require a page reload.
- `frontend/src/main.tsx` bootstraps initial data fetches before the first render, which could contribute to initial load behavior but is not itself a repeating refresh.

## What I found so far
- `frontend/src/hooks/useIdleRefresh.ts` sets a timer on user activity and reloads the whole page with `window.location.reload()` when idle time expires.
- `frontend/src/App.tsx` mounts `useIdleRefresh()` on every app render.
- `frontend/src/hooks/useSSE.ts` already handles live updates via EventSource and falls back to polling `/api/devices` when SSE drops.
- There were no other obvious `reload`/`refresh` calls found in the front-end source scan.

## Proposed approach
1. Confirm the user-facing symptom is tied to the idle refresh timer, not a route change or forced reconnect.
2. Inspect where `idleRefreshMinutes` is configured and whether the default or persisted value is causing the flash frequency.
3. Review whether the desired behavior is one of the following:
   - no refresh at all,
   - silent data sync in place,
   - or a soft refresh that preserves UI state.
4. If needed, adjust the app to avoid `window.location.reload()` and instead refresh data through existing store/SSE/polling mechanisms.
5. Verify the new behavior with targeted tests or a manual browser flow.

## Likely files to change
- `frontend/src/hooks/useIdleRefresh.ts`
- `frontend/src/store/settingsStore.ts`
- `frontend/src/App.tsx` if the hook should be gated or removed
- `frontend/src/hooks/useSSE.ts` if the refresh strategy needs to be centralized
- Possibly related UI/settings components if the idle refresh is user-configurable elsewhere

## Tests / validation
- Search for all occurrences of `window.location.reload`, `location.reload`, or related hard refresh code in `frontend/src`.
- Run the front-end test suite or at least the dashboard Playwright tests in `frontend/tests/dashboard.spec.ts` after any change.
- Manually verify that navigating the dashboard no longer flashes while idle update behavior still works.

## Risks / tradeoffs
- Removing the hard reload could leave some stale state if there are edge cases not covered by SSE or polling.
- A soft refresh may need explicit store reset or refetch logic to keep the UI consistent after long idle periods.
- If the flash is actually caused by a route redirect or a remount from React strict mode in development, the fix will differ from removing idle reloads.

## Open questions
- Should the dashboard ever auto-refresh at all, or should it rely entirely on live updates and polling?
- Is the flash happening only after idle time passes, or also during normal navigation?
- Should the existing idle refresh setting become a soft data refresh instead of a browser reload?
