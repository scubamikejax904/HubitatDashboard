# Plan: Re-architect GPS tracking with polling and interval-based logic

## Goal
Migrate the GPS tracking logic from a simple alarm-triggered model to a polling-based model (with a user-selectable polling interval) that evaluates location changes against a distance threshold, while maintaining an interval-based fallback for periodic reporting.

## Proposed Strategy
1. **Periodic Polling**: Switch from a single-shot alarm to a foreground service using `FusedLocationProviderClient`. The polling interval will be in seconds, configurable by the user (defaulting to 10 seconds).
2. **Dynamic Heuristics**:
   - Every polling interval (e.g., 10s), evaluate the movement:
     - If distance traveled since last POST >= `gpsMinDistanceMiles`, perform `POST` and update `lastPostedLocation`.
     - Else if time elapsed since last POST >= `gpsTrackingInterval` (in minutes), force `POST` to ensure interval heartbeats still occur.
3. **Configuration Strategy**: 
   - Add `gpsPollingIntervalSeconds` to `SettingsRepository.kt` (persistable via SharedPreferences).
   - Expose this setting in the UI.

## Step-by-Step Plan
1. **Phase 0: Inspection**
   - Examine `LocationTrackerService.kt` and `SettingsRepository.kt`.
2. **Phase 1: Settings Update**
   - Add `gpsPollingIntervalSeconds` to `SettingsRepository.kt`.
   - Update `SettingsViewModel.kt` to allow setting this value.
3. **Phase 2: Service Refactor**
   - Update `LocationTrackerService.kt` to initialize the `FusedLocationProviderClient` with a location request matching the user's polling interval.
   - Implement the distance delta calculation within the location callback.
   - Ensure the service is correctly started and managed as a foreground service with the necessary `Wakelock` / `Notification` logic.
4. **Phase 3: Logic Implementation**
   - Implement the conditional logic (distance threshold vs. forced interval heartbeat).
5. **Phase 4: UI Updates**
   - Add a configuration field to `LocationTrackerScreen.kt` for the polling interval (in seconds).
6. **Validation**
   - Monitor `adb logcat` output during test commutes to confirm upload behavior vs. distance/time conditions.

## Files Likely to Change
- `android/app/src/main/java/com/tim/hubitatdash/service/LocationTrackerService.kt`
- `android/app/src/main/java/com/tim/hubitatdash/data/repository/SettingsRepository.kt`
- `android/app/src/main/java/com/tim/hubitatdash/viewmodel/LocationTrackerViewModel.kt`
- `android/app/src/main/java/com/tim/hubitatdash/ui/tracker/LocationTrackerScreen.kt`

## Blockers / Risks
- **Battery Impact**: Frequent polling (10s) needs careful management. Ensure location precision settings are set to balanced/accuracy balanced rather than force high-accuracy satellite, to optimize battery.
- **Background Restrictions**: Android 14+ background location limits may require strict compliance with foreground service notification requirements.

---
*Updated on 2026-06-11 to include user-defined polling interval.*
