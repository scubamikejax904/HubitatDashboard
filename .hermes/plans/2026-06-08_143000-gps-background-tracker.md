# GPS Background Tracker Feature

## Goal

Add a power-efficient, highly reliable background GPS tracker to the existing HubitatDashboard
Android app. Every N minutes (user-configurable), the phone captures its current GPS coordinates
and POSTs them to a Google Apps Script Web App endpoint, which appends a row to a Google Sheet.

---

## Current Context & Assumptions

- **Project**: `com.timshubet.hubitatdashboard`
- **Architecture**: Kotlin + Jetpack Compose, Hilt DI, version catalog (`libs.versions.toml`)
- **Existing SDK**: compileSdk 35, targetSdk 35, minSdk 26
- **Existing dependencies already present**: OkHttp 5.0.0-alpha.14, Retrofit, Hilt 2.57.1, Encrypt
edSharedPreferences
- **Existing service pattern**: `service/RingNotificationListener.kt` (NotificationListenerService)
- **No existing location dependency**: `play-services-location` is NOT yet in the project
- **DI pattern**: Hilt modules in `di/`, `@Inject` constructors, `@AndroidEntryPoint` on
  services/activities
- **Network client**: Single `OkHttpClient` provided via `NetworkModule` singleton

---

## Proposed Architecture

```
User toggles ON in Settings
        |
        v
  AlarmReceiver (BroadcastReceiver)
  setExactAndAllowWhileIdle → wakes at interval
        |
        v
  LocationTrackerService (Foreground Service)
    - shows persistent notification (channel "gps_tracker")
    - uses FusedLocationProviderClient.getLastLocation / requestSingleUpdate
    - POSTs JSON to Google Apps Script Web App URL
    - stops self after successful send
        |
        v
  AlarmReceiver re-schedules next alarm before stopping service
```

**Why AlarmManager over WorkManager?**
- WorkManager has a 15-min minimum; user may want 5-min intervals.
- `setExactAndAllowWhileIdle` survives Doze Mode and is precise.
- The foreground service guarantees execution time for the network call.

---

## Step-by-Step Plan

### Step 1: Add Dependencies

**File**: `gradle/libs.versions.toml`

Add under `[versions]`:
```toml
playServicesLocation = "21.3.0"
coroutinesPlayServices = "1.9.0"
accompanist = "0.34.0"
```

Add under `[libraries]`:
```toml
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
kotlinx-coroutines-play = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutinesPlayServices" }
```

**File**: `app/build.gradle.kts`

Add in `dependencies {}`:
```kotlin
implementation(libs.play.services.location)
implementation(libs.kotlinx.coroutines.play)
```

---

### Step 2: Android Manifest Updates

**File**: `app/src/main/AndroidManifest.xml`

Add permissions (after existing ones):
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

Add inside `<application>`:
```xml
<service
    android:name=".service.LocationTrackerService"
    android:exported="false"
    android:foregroundServiceType="location" />

<receiver
    android:name=".service.AlarmReceiver"
    android:exported="false" />

<receiver
    android:name=".service.BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

### Step 3: Settings Persistence for Tracker Config

**File**: `data/repository/SettingsRepository.kt` (modify existing)

Add new keys and getters/setters:
```kotlin
const val KEY_GPS_TRACKING_ENABLED = "gps_tracking_enabled"
const val KEY_GPS_TRACKING_INTERVAL = "gps_tracking_interval"    // minutes, default 15
const val KEY_GPS_APPS_SCRIPT_URL = "gps_apps_script_url"
const val KEY_GPS_DEVICE_NAME = "gps_device_name"

val gpsTrackingEnabled: Boolean get() = prefs.getBoolean(KEY_GPS_TRACKING_ENABLED, false)
val gpsTrackingInterval: Int get() = prefs.getInt(KEY_GPS_TRACKING_INTERVAL, 15)
val gpsAppsScriptUrl: String get() = prefs.getString(KEY_GPS_APPS_SCRIPT_URL, "") ?: ""
val gpsDeviceName: String get() = prefs.getString(KEY_GPS_DEVICE_NAME, Build.MODEL) ?: ""

fun setGpsTrackingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_GPS_TRACKING_ENABLED, enabled).apply()
fun setGpsTrackingInterval(minutes: Int) = prefs.edit().putInt(KEY_GPS_TRACKING_INTERVAL, minutes).apply()
fun setGpsAppsScriptUrl(url: String) = prefs.edit().putString(KEY_GPS_APPS_SCRIPT_URL, url).apply()
fun setGpsDeviceName(name: String) = prefs.edit().putString(KEY_GPS_DEVICE_NAME, name).apply()

fun isGpsTrackerConfigured(): Boolean = gpsAppsScriptUrl.isNotBlank() && gpsDeviceName.isNotBlank()
```

---

### Step 4: AlarmScheduler Utility

**New file**: `service/AlarmScheduler.kt`

- Takes `Context`, reads interval from `SettingsRepository`
- Uses `AlarmManager.setExactAndAllowWhileIdle()` with `PendingIntent` targeting `AlarmReceiver`
- Calculates next alarm time from interval
- Methods: `scheduleNext()`, `cancel()`
- Injected via constructor (not Hilt — it's a simple utility used from static contexts)
  with a `Context`-based factory or manual instantiation

---

### Step 5: AlarmReceiver (BroadcastReceiver)

**New file**: `service/AlarmReceiver.kt`

```kotlin
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Start the foreground service which will:
        // 1. Get GPS fix
        // 2. POST to Apps Script
        // 3. Re-schedule next alarm
        // 4. stopSelf()
        val serviceIntent = Intent(context, LocationTrackerService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
```

---

### Step 6: BootReceiver

**New file**: `service/BootReceiver.kt`

Re-schedules the alarm after device reboot (if tracking was enabled):
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = // read encrypted prefs directly or use AlarmScheduler
            if (/* gpsTrackingEnabled */) {
                AlarmScheduler(context).scheduleNext()
            }
        }
    }
}
```

**Note**: BootReceiver cannot easily use Hilt injection for SettingsRepository (encrypted prefs).
It should read the boolean directly from EncryptedSharedPreferences or use a simpler non-
encrypted preference for the "enabled" flag that it can read without crypto overhead.

---

### Step 7: LocationTrackerService (Foreground Service)

**New file**: `service/LocationTrackerService.kt`

Key responsibilities:
1. Call `startForeground()` immediately with a persistent notification
2. Request a single GPS fix via `FusedLocationProviderClient`
3. POST `{ timestamp, lat, lng, device }` to the Apps Script URL
4. Re-schedule the next alarm via `AlarmScheduler`
5. Call `stopSelf()`

```kotlin
class LocationTrackerService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var settingsRepository: SettingsRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        // Hilt injection via @AndroidEntryPoint OR manual field init
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Getting GPS fix..."))

        serviceScope.launch {
            try {
                val location = getDeviceLocation()
                if (location != null) {
                    updateNotification("Sending: ${location.latitude}, ${location.longitude}")
                    postLocation(location)
                    updateNotification("Last sent: ${formatTime()}")
                } else {
                    updateNotification("No GPS fix available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tracker error", e)
                updateNotification("Error: ${e.message}")
            } finally {
                AlarmScheduler(this@LocationTrackerService).scheduleNext()
                // Stop after a brief delay so the notification shows the final state
                delay(3000)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun getDeviceLocation(): Location? {
        // Use await() from kotlinx-coroutines-play-services
        return suspendCancellableCoroutine { cont ->
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                cont.resume(location)
            }.addOnFailureListener { cont.resume(null) }
        }
    }

    private suspend fun postLocation(location: Location) {
        val url = settingsRepository.gpsAppsScriptUrl
        val json = JSONObject().apply {
            put("timestamp", Instant.now().toString())
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("device", settingsRepository.gpsDeviceName)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "Apps Script returned HTTP ${response.code}")
        }
    }
}
```

**Hilt integration consideration**: Services can use `@AndroidEntryPoint` but
`LocationTrackerService` must be careful to call `startForeground()` before Hilt finishes
injecting. The safest pattern: make it `@AndroidEntryPoint` (Hilt injects before `onCreate`
returns on API 26+), and call `startForeground()` at the top of `onStartCommand()`.

---

### Step 8: Google Apps Script (standalone, deployed separately)

This is NOT part of the Android build — it's a Google Apps Script project the user deploys
manually.

**Script** (`Code.gs`):
```javascript
const SHEET_ID = 'YOUR_GOOGLE_SHEET_ID_HERE';
const SHEET_NAME = 'GPS_Log';

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    const sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName(SHEET_NAME);

    sheet.appendRow([
      data.timestamp || new Date().toISOString(),
      data.latitude,
      data.longitude,
      data.device || 'Unknown'
    ]);

    return ContentService
      .createTextOutput(JSON.stringify({ status: 'ok' }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ status: 'error', message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet() {
  return ContentService.createTextOutput('GPS Tracker endpoint is active.');
}
```

**Setup instructions** (to be documented in-plan or in a separate README snippet):
1. Create a new Google Sheet with columns: Timestamp | Latitude | Longitude | Device
2. Open Extensions → Apps Script
3. Paste the script, replace `SHEET_ID`
4. Deploy → New deployment → Web app
5. Execute as: Me  |  Who has access: Anyone
6. Copy the deployment URL → paste into the app's Settings screen

---

### Step 9: UI — GPS Tracker Settings (new screen or section)

**Option A**: Add a new "GPS Tracker" section to the existing Settings screen.
**Option B**: Create a dedicated `LocationTrackerScreen` with its own nav route.

Recommendation: **Option B** — mirrors the Ring Listener / Hubitat Notifications pattern.

**New files**:
- `ui/tracker/LocationTrackerScreen.kt` — Compose screen with toggles and text fields
- `viewmodel/LocationTrackerViewModel.kt` — Hilt ViewModel

UI elements:
- Toggle: Enable/disable GPS tracking
- TextField: Apps Script Web App URL
- TextField: Device name (prefilled with `Build.MODEL`)
- Slider or dropdown: Interval (5, 10, 15, 30, 60 minutes)
- Button: "Test Now" (immediate single ping)
- Status text: "Last ping: ..." / "Next scheduled: ..."
- Permission status indicators (fine location, background location)

**Navigation**:
Add `const val GPS_TRACKER = "gps_tracker"` to `NavRoutes`, wire into `MainScreen` NavHost
and the bottom nav or settings gear.

---

### Step 10: Runtime Permission Handling

In `LocationTrackerScreen` or `LocationTrackerViewModel`:
1. Request `ACCESS_FINE_LOCATION` at runtime (standard Android 6+ flow)
2. If tracking is enabled, also request `ACCESS_BACKGROUND_LOCATION` (Android 10+, requires
   separate request, goes to system Settings screen — cannot be granted from in-app dialog)
3. Show clear messaging about why background location is needed

---

## Files to Create

| File | Purpose |
|------|---------|
| `service/LocationTrackerService.kt` | Foreground service: GPS fix → POST → reschedule |
| `service/AlarmScheduler.kt` | AlarmManager utility: schedule/cancel |
| `service/AlarmReceiver.kt` | BroadcastReceiver: alarm fires → start service |
| `service/BootReceiver.kt` | BroadcastReceiver: reschedule after reboot |
| `ui/tracker/LocationTrackerScreen.kt` | Compose UI for tracker settings |
| `viewmodel/LocationTrackerViewModel.kt` | ViewModel for tracker screen |

## Files to Modify

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add play-services-location, coroutines-play-services |
| `app/build.gradle.kts` | Add new dependency implementations |
| `app/src/main/AndroidManifest.xml` | Add permissions + service/receiver declarations |
| `data/repository/SettingsRepository.kt` | Add GPS tracker config keys/getters/setters |
| `ui/shell/NavGraph.kt` | Add `GPS_TRACKER` route constant |
| `ui/shell/MainScreen.kt` | Add composable route + nav entry |

---

## Tests / Validation

1. **Unit tests**: `AlarmScheduler` schedule math
2. **Manual tests**:
   - Enable tracking, verify notification appears immediately
   - Lock phone, wait for interval, verify row appears in Google Sheet
   - Reboot phone, verify alarm rescheduled
   - Disable tracking, verify alarm cancelled (no more pings)
   - Kill app from recents, verify service restarts on next alarm
3. **Battery**: Use `adb shell dumpsys batterystat | grep hubitat` after 24h to check impact
4. **Network failure**: Test with airplane mode — service should not crash, should log error
   and re-schedule
5. **Location edge cases**: Test indoors (no GPS fix), test with WiFi-only (coarse fallback)

---

## Risks, Tradeoffs & Open Questions

### Risks

| Risk | Mitigation |
|------|-----------|
| OS kills service despite foreground | `foregroundServiceType="location"` + exact alarm +
  `START_NOT_STICKY` (alarm reschedules) |
| Google rejects app for background location in Play Store | N/A — sideloaded APK, not Play
  Store |
| Apps Script URL leaks via decompilation | Sheet is user's own; URL is effectively a secret.
  Store in EncryptedSharedPreferences (already encrypted). |
| Doze mode defers alarm up to 9 min | `setExactAndAllowWhileIdle` is exempt from Doze
  batching, but Android 12+ may still throttle. Worst case: 15-min interval may fire at ~24
  min. |
| `FusedLocationProviderClient` returns null | Fall back to `getLastLocation()`; if still null,
  log and skip this cycle (alarm will re-fire next interval) |
| BootReceiver cannot read EncryptedSharedPreferences easily | Use a lightweight non-encrypted
  `SharedPreferences` for just the "enabled" flag, readable from BootReceiver without crypto
  key |

### Tradeoffs

- **AlarmManager vs WorkManager**: AlarmManager with `setExactAndAllowWhileIdle` is more
  precise but battery-hungry at very short intervals. For ≥10 min intervals this is fine. If
  the user picks 1 min, consider switching to a long-lived foreground service with an internal
  timer instead.
- **Foreground notification visibility**: The persistent notification is intrusive but necessary
  for reliability. We can minimize it with `IMPORTANCE_LOW` so it doesn't make a sound/vibrate.

### Open Questions for User

1. Should the tracker screen be its own nav route (like Ring Listener), or a section within
   Settings? **Recommendation: Own route** for consistency.
2. Should there be a "last N pings" log in the UI? **Recommendation: Yes**, small list of
   recent timestamps/lat/lng for debugging.
3. Minimum interval enforcement? **Recommendation: 5 minutes** (below this, battery drain is
   aggressive and Google may throttle exact alarms).
4. Should the Apps Script URL be validated (HEAD request / `doGet` check) before saving?
   **Recommendation: Yes**, simple "Test Connection" button that hits `doGet` and checks for
   200.

---

## Execution Order

1. Add dependencies → verify build compiles
2. Update manifest
3. Implement `AlarmScheduler`, `AlarmReceiver`, `BootReceiver`
4. Add settings keys to `SettingsRepository`
5. Implement `LocationTrackerService`
6. Build `LocationTrackerViewModel` + `LocationTrackerScreen`
7. Wire navigation into `MainScreen` / `NavGraph`
8. Manual testing on device
9. Deploy Google Apps Script and verify end-to-end
