# Plan: Update Android GPS Tracker Upload Logic

The goal is to modify the location tracking logic in `LocationTrackerService.kt` to allow an upload if EITHER the distance threshold is exceeded OR the time interval since the last upload has elapsed.

## Current Context
- The project is an Android app with the core service in `android/app/src/main/java/com/tim/hubitatdash/service/LocationTrackerService.kt`.
- The current trigger logic is concentrated in `shouldPostLocation(newLocation: android.location.Location)`.
- It currently only compares distance (`distanceMiles > minDistance`).

## Proposed Approach
1. Add `KEY_LAST_UPLOAD_TIME` to the `companion object` constants in `LocationTrackerService.kt`.
2. In `shouldPostLocation(newLocation)`, add a check for the time elapsed. 
   - Get `lastUploadTime` from `SharedPreferences`.
   - Calculate `val timeElapsedMinutes = (System.currentTimeMillis() - lastUploadTime) / 60000`.
   - Retrieve the allowed interval from `settingsRepository.gpsTrackingInterval` (assuming this stores the interval in minutes or a compatible format).
   - Update `shouldPost` to: `val shouldPost = (distanceMiles > minDistance) || (timeElapsedMinutes >= settingsRepository.gpsTrackingInterval)`.
3. Update `postLocation` to save the current timestamp to `KEY_LAST_UPLOAD_TIME` upon a successful upload.

## Step-by-Step Plan
1. **Edit Constants**: Add `KEY_LAST_UPLOAD_TIME` to `companion object`.
2. **Modify `shouldPostLocation`**: 
   - Retrieve `lastUploadTime`.
   - Calculate `timeElapsed`.
   - Integrate time condition into the `shouldPost` check.
   - Update the status message to reflect which condition triggered the post.
3. **Modify `postLocation`**:
   - Save the current time when the location POST is successful.
4. **Validation**: I will perform a smoke test of the logic change (logic structure check) by reading the code after applying these changes.

## Files to Change
- `android/app/src/main/java/com/tim/hubitatdash/service/LocationTrackerService.kt`

## Risks & Blockers
- **Assumptions**: I am assuming `settingsRepository.gpsTrackingInterval` provides the correct interval in minutes (as often handled in settings). I will check what `gpsTrackingInterval` is (via `SettingsRepository` if possible, otherwise rely on naming).
