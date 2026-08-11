# Plan: GPS Tracker Upload Logic Refinement

The goal is to modify the GPS tracker upload logic so that an upload is triggered when either the minimum distance threshold is met OR the configured time interval has elapsed.

## Current Context
- Hubitat Dashboard backend (`backend/src/`) serves data but does not contain the *phone-side* upload logic.
- The phone uploads location data to a Google Sheets HTTP endpoint (likely via a Google Apps Script web app or an direct integration).
- The *backend* part of this repo retrieves and displays this data.
- The user wants to change *how* the phone triggers the uploads.

## Assumptions
- The phone-side logic (e.g., Tasker, Android App) controls the frequency and trigger conditions of the uploads.
- The server-side code (in `backend/src/`) is not responsible for the upload frequency.

## Proposed Approach
1. Confirm with the user if the logic resides in a Google Apps Script or an Android app (e.g., Tasker/Automate/Custom App) since the backend does not handle it.
2. If it is an Android app/Tasker: I will provide the steps or logic changes needed for their Tasker profile or code to implement a time-OR-distance trigger.
3. If it is a Google Apps Script: I will suggest how to add a timestamp check in that script to ignore or process uploads based on the time interval.

## Step-by-Step Plan
1. **Clarify Source**: Ask the user (confirming my search results) whether the upload logic is inside an Android app, Tasker task, or a Google Apps Script. 
2. **Review Logic Requirement**:
   - If Tasker: Provide the structure for a "Notify/Wait/Condition" logic based on a time variable and a distance calculation.
   - If Google Apps Script: Provide a code snippet for the `doPost` function in the Apps Script to timestamp the last upload and perform the time-based trigger.

## Risks & Blockers
- **Blocker**: The implementation relies on the user's phone-side configuration or a third-party Google Apps Script which is NOT in this repository.

## Files to Inspect
- None in this repository are relevant to the actual upload trigger logic.
