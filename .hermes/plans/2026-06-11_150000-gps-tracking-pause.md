# Plan: Pause GPS tracking uploads when stationary

## Goal
Implement a motion-based filtering mechanism for the GPS tracker service. Uploads should pause if the device has not moved significantly (> 100 feet/approx. 0.019 miles) since the last upload. However, once the threshold is crossed, movement-based uploads should resume, along with existing periodic interval uploads.

## Current Context
- Service: `android/app/src/main/java/com/tim/hubitatdash/service/LocationTrackerService.kt`
- logic to check: `shouldPostLocation(newLocation: android.location.Location)` method.
- Current behavior: `shouldPost` is true if `enoughDistance` (distance > `minDistance`) OR `enoughTime` (timeElapsedMinutes >= `intervalMinutes`).
- The user wants to: "Pause ... if device has not moved more than 100 feet."

## Configuration Strategy
- Ensure `gpsMinDistanceMiles` is exposed in `SettingsRepository` and adjustable. Current code uses `settingsRepository.gpsMinDistanceMiles`.
- The logic needs to be strictly: "If moved > threshold AND (Time interval met OR ...), upload."
- Actually, the user's intent implies a state: "If I am not moving, don't upload."
- Proposed Logic:
  - `val isMoving = distanceMiles > 0.019` (100 feet in miles).
  - `val isTimeElapsed = timeElapsedMinutes >= intervalMinutes`.
  - Condition: `if (!isMoving && !isTimeElapsed) return false // Stay stationary, skip upload`

## Proposed Approach
Modify `shouldPostLocation` in `LocationTrackerService.kt`.

1.  Keep tracking the `lastLat`, `lastLng` regardless if we upload or not, to maintain a "last known anchored position" to compare against for future 100ft movement.
2.  Refined logic:
    - `val MIN_MOVEMENT_MILES = 0.019; // ~100 feet`
    - `val movedEnough = distanceMiles > MIN_MOVEMENT_MILES`
    - `val postCondition = (isTimeElapsed || movedEnough)`
    - BUT wait, if we don't upload because not moved, we should NOT update `KEY_LAST_UPLOAD_TIME`.
    - Also, we should potentially store a "last posted location" separately from the "last known location" to correctly calculate if we have moved away from the *last posted* position once we decide to resume. Actually `saveLastLocation` is currently called only after successful POST (line 95), so `lastLat`/`lastLng` in prefs *are* the last posted location. This is good.

## Implementation Steps
1. Define the 100ft static constant in `LocationTrackerService.kt`.
2. Update `shouldPostLocation`:
   - Calculate `distanceMiles` from the last *posted* location (which is already done via `prefs`).
   - `val hasSignificantlyMoved = distanceMiles > 0.019`
   - `val intervalDue = timeElapsedMinutes >= intervalMinutes`
   - `shouldPost = intervalDue || hasSignificantlyMoved`
3.  Wait, if the user says "If it is paused, only upload again when the device move more than 100 feet", this is the exactly same logic as `hasSignificantlyMoved`.
4.  Test logic:
    - If `not movedEnough` AND `not intervalDue`: skip. Correct.
    - If `movedEnough`: upload. Correct.
    - If `intervalDue`: upload. Correct.

## Improved Ideas (Addressing user's "missing anything")
- **Battery optimization**: If not moving, allow the device to enter deeper sleep since we don't need high-frequency wake-ups by the `AlarmScheduler`.
- **Drift mitigation**: If stationary for a _very_ long time (e.g., 24 hours), force an upload anyway to confirm the device is still alive and reporting.
- **GPS noise filter**: 100ft is a good threshold, ensure it's not smaller than the average GPS jitter (which can be 10-30ft).

## Files to Change
- `android/app/src/main/java/com/tim/hubitatdash/service/LocationTrackerService.kt`

## Verification
- Run a small test in `LocationTrackerService` by simulating a location with small jitter vs. a simulated jump > 100ft.

## Risks
- If the phone doesn't move for days, the user might think it's broken. The "force upload" after 24h is a good safeguard.
