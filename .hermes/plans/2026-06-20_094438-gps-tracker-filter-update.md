# GPS Tracker Page Filter Update

## Goal
Update the GPS tracker page to:
1. Set the default filter to "today" 
2. Stop drawing lines between track points

## Current Context / Assumptions

- Project: HubitatDashboard Android app (based on the file paths identified)
- Tech stack: Kotlin with Jetpack Compose UI
- Location of main tracker screen: `android/app/src/main/java/com/tim/hubitatdash/ui/tracker/LocationTrackerScreen.kt`
- Configuration stored in SharedPreferences via SettingsRepository
- The main GPS tracking functionality already exists but lacks filtering and line drawing options

## Configuration Strategy

- The app uses SharedPreferences for storing configuration parameters
- Values are persisted through the `SettingsRepository` class 
- Filters and map display settings would need to be added/modified in the settings persistence layer

## Proposed Approach

Based on analyzing the code, it appears this is a GPS tracker application that likely:
1. Configures tracking behavior but doesn't currently have a page showing historical tracks or map visualization
2. The screen shown focuses on configuration (enable/disable, interval, distance filters, etc.)

The problem might be:
- Either the implementation details are missing from the current codebase
- Or we need to search more specifically for existing mapping functionality

I see that `LocationTrackerScreen.kt` has configuration options but no actual map display. There may not be a separate "tracking page" with map visualization. The user might be referring to either:
1. A different screen not present in the current code, or 
2. Some other app component I haven't found yet

Let me check if there's another part of the codebase containing map functionality that displays past tracks.

## Step-by-step Plan

### Phase 0: Discovery and Understanding
- Examine how GPS tracking settings work to understand the current configuration
- Locate where any map visualization or historical tracking view might exist
- Identify which file contains filter settings and mapping logic for track points

### Phase 1: Determine Map UI and Tracking Logic Location  
- Search Android manifest or navigation setup to find where "tracking page" is referenced
- Look for any screen that displays tracks, routes, or map visualization of GPS data

### Phase 2: Update Default Filter 
- Identify the settings/initialization used for filter default value
- Ensure "today" becomes the default filter option

### Phase 3: Disable Line Drawing
- Find where the track points are drawn on a map  
- Remove or disable line drawing functionality between points

## Files likely to change

1. `android/app/src/main/java/com/tim/hubitatdash/ui/tracker/LocationTrackerScreen.kt` - Main tracker UI 
2. `android/app/src/main/java/com/tim/hubitatdash/viewmodel/LocationTrackerViewModel.kt` - Configuration handling 
3. `android/app/src/main/java/com/tim/hubitatdash/data/repository/SettingsRepository.kt` - Settings persistence
4. Any map rendering components or screens that might be used to show tracks (could not locate in current search)

## Tests / Validation

- Ensure application compiles and builds properly after changes
- Verify default filter is "today" when page is loaded
- Verify no lines are drawn between points in the track visualization (if present)
- Test that existing functionality for config settings remains working

## Risks, Tradeoffs, and Open Questions

1. **Missing UI Components**: Since I cannot directly see a map display or filtering UI component for GPS tracks in the current search, I may need to understand better what screen is referenced by "the GPS tracker page" as mentioned in the request.

2. **Default Filter Implementation**: Need to understand exactly where the default filter value gets initialized and how it relates to existing time filtering settings.

3. **Map/Line Logic**: Need to locate any rendering code for maps and track drawing that actually creates lines between points.

4. **User Experience Impact**: Changing defaults might affect users who were previously relying on a different default, though "today" seems like a reasonable default.

The implementation will likely require either locating this missing map functionality or clarifying what screen exactly needs to be modified.