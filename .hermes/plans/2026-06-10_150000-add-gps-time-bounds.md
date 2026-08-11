# Plan: Add Time-Based Restrictions to GPS Tracker

## Goal
Modify the Android GPS Tracker service to prevent interval-based location reporting outside a user-defined configurable time range ([startHour, endHour]).

## Current Context / Assumptions
- Project: `HubitatDashboard` (Android module).
- Location Tracking: Managed by `LocationTrackerService` and configured via `LocationTrackerViewModel`.
- Configuration: Managed via `SettingsRepository` (SharedPreferences).
- The service correctly schedules the next fix using `AlarmScheduler`. 
- Logic for interval reporting resides in `LocationTrackerService.shouldPostLocation`.

## Configuration Strategy
- Add two new keys to `SettingsRepository`: `KEY_GPS_START_HOUR` (default `0`) and `KEY_GPS_END_HOUR` (default `23`).
- Add persistent storage helpers in `SettingsRepository` and `LocationTrackerViewModel` to expose these values to the UI.
- No changes to existing `.env` or external files; store in the app's existing encrypted SharedPreferences.

## Proposed Approach
1. **Repository Update**: Add `gpsStartHour` and `gpsEndHour` properties to `SettingsRepository` with setter methods.
2. **ViewModel Update**: Add `setStartHour` and `setEndHour` methods to `LocationTrackerViewModel` to persist settings and update UI state.
3. **Service Logic Update**: Modify `LocationTrackerService.onStartCommand` to check current time against these bounds. If the current time is outside the permitted range, it should skip the `postLocation` call (or at least check if it's the right time to do so) and immediately re-schedule the next alarm.
4. **UI Update**: Add two text fields or dropdowns in `LocationTrackerScreen` to let the user set these integer values.

## Step-by-Step Plan
1. **Phase 1: Repository & ViewModel**
   - Add new `KEY_` constants to `SettingsRepository.kt`.
   - Update `SettingsRepository.kt` with property getters and setters.
   - Add new properties to `TrackerUiState` in `LocationTrackerViewModel.kt`.
   - Update `loadState()` and state-management methods in `LocationTrackerViewModel.kt`.
2. **Phase 2: Service Logic (The core gating)**
   - In `LocationTrackerService.kt`, update `onStartCommand` to check if `LocalTime.now()` is between the configured start and end hour.
   - If outside the range, skip POST and re-schedule.
3. **Phase 3: UI Implementation**
   - Add two `OutlinedTextField` components (keyboard: number) to `LocationTrackerScreen.kt` for `startHour` and `endHour`.
   - Bind them to the new VM methods.
4. **Verification**
   - Verify that enabling/disabling the tracker respects the time bounds.
   - Check that UI properly reflects the range.

## Files likely to change
- `android/app/src/main/java/com/tim/hubitatdash/data/repository/SettingsRepository.kt`
- `android/app/src/main/java/com/tim/hubitatdash/viewmodel/LocationTrackerViewModel.kt`
- `android/app/src/main/java/com/tim/hubitatdash/service/LocationTrackerService.kt`
- `android/app/src/main/java/com/tim/hubitatdash/ui/tracker/LocationTrackerScreen.kt`

## Risks, tradeoffs, and open questions
- **Timezone**: `LocalTime.now()` follows the device's system clock, which matches the user's intent.
- **Service trigger**: If the service wakes up via an alarm, it needs to ensure it doesn't accidentally run if closed or restricted — logic must remain robust.
- **Edge cases**: What if `start > end` (e.g., overnight 20:00 - 05:00)? The logic should support cross-midnight ranges if possible, or force valid input. I'll stick to a simple `start < end` validator for version 1.0 to keep it manageable.
