# Plan: Update GPS Upload Logic

The goal is to modify the GPS upload logic so that uploads are triggered by the EARLIER of either:
1. Distance threshold met
2. Time interval elapsed

## Current Context
- The user is using an Android application that performs the location tracking and uploads automatically.
- The user implies they have access to the source of this app or its configuration.
- The repository provided is not the app itself; we need to identify where to find the source code for this app.

## Proposed Approach
1. Ask the user for the code/repo location of the Android application.
2. Once the source is located, identify the logic that handles location updates and the upload trigger.
3. Propose a code change that tracks `lastUploadTimestamp` and compares `Date.now() - lastUploadTimestamp` against the `interval` setting in the trigger condition.

## Step-by-Step Plan
1. **Locate Source**: Request the path or repository for the Android application source code.
2. **Review Upload Logic**: Inspect the existing implementation for distance-based triggers.
3. **Draft Modification**: Insert a check for the time interval and update the trigger logic to: `if (distance >= minDistance || timeDelta >= interval)`.
4. **Validate**: Review the logic for edge cases (e.g., initial upload, interval settings).

## Risks & Blockers
- **Blocker**: I do not currently see the source for the Android app in the `HubitatDashboard` repository.
- **Risks**: I cannot modify the logic if I cannot see the implementation.

## Files to Inspect
- Pending user input regarding the location of the Android app source code.
