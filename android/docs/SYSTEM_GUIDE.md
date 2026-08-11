# Hubitat Dashboard: Developer and System Guide

This guide provides instructions for building the Android application and managing the associated backend system services.

## 1. Building the Android Application

The Android app is located in the `android/` directory. It uses a custom `gradlew` script for building.

### Prerequisites
- JDK 17 installed.
- Android SDK installed (expected at `/opt/android-sdk`).

### Build Steps
1. Navigate to the project `android` directory.
2. Run the build command:
   ```bash
   bash gradlew assembleDebug
   ```
3. The resulting APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Managing Backend System Services

The backend runs as a systemd user service. 

### Service Management Commands
To manage the `hubitat-dashboard` service:

- **Start the service:**
  ```bash
  systemctl --user start hubitat-dashboard
  ```
- **Stop the service:**
  ```bash
  systemctl --user stop hubitat-dashboard
  ```
- **Restart the service:**
  ```bash
  systemctl --user restart hubitat-dashboard
  ```
- **Check service status:**
  ```bash
  systemctl --user status hubitat-dashboard
  ```
- **View service logs:**
  ```bash
  journalctl --user -u hubitat-dashboard
  ```

*Note: Enable the service to start on login/boot with `systemctl --user enable hubitat-dashboard`.*

## 3. Managing Android Devices via ADB

Use these commands to manage the Hubitat Dashboard app on connected devices:

- **List connected devices:**
  ```bash
  adb devices
  ```
- **Install the app:**
  ```bash
  adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk
  ```
- **Uninstall the app:**
  ```bash
  adb -s <device-id> uninstall com.tim.hubitatdash
  ```
- **Check GPS tracker logs:**
  ```bash
  adb -s <device-id> logcat -s GPSTracker
  ```