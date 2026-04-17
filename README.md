# Android Usage Timer Widget

An Android home-screen widget for tracking accumulated usage time (like vinyl cartridge playtime, equipment runtime, etc.). Create multiple independent stopwatches with named instances, all persisting across reboots.

## Features

✅ **Count-up stopwatch timers** — Track accumulated usage time  
✅ **Multiple independent timers** — One for each cartridge/item (e.g., "Cartridge A", "Cartridge B")  
✅ **Persistent state** — Timers survive widget deletion and device reboots  
✅ **Restore timers** — When adding a new widget, optionally restore a previously-created timer  
✅ **Display format** — Shows up to 9999:59:59 (hours:minutes:seconds)  
✅ **Start/Stop/Reset** — Simple controls on the widget  
✅ **Edit capability** — Change timer name and manually adjust elapsed time if needed  
✅ **Background tracking** — Continues counting even if widget isn't visible  

## Project Structure

```
timers/
├── app/
│   ├── build.gradle           # App-level Gradle configuration
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/timers/widget/
│   │   │   ├── TimerWidgetProvider.java    # Main widget provider
│   │   │   ├── TimerService.java           # Background stopwatch service
│   │   │   ├── TimerConfigActivity.java    # Timer setup/edit UI
│   │   │   └── TimerData.java              # Data storage utilities
│   │   └── res/
│   │       ├── layout/
│   │       │   ├── timer_widget_layout.xml # Widget UI layout
│   │       │   └── timer_edit_dialog.xml   # Edit/create dialog
│   │       ├── xml/
│   │       │   └── timer_widget_info.xml   # Widget metadata
│   │       └── values/
│   │           ├── strings.xml
│   │           └── styles.xml
├── build.gradle                # Project-level Gradle configuration
├── settings.gradle             # Gradle settings
└── README.md
```

## Building

1. **Open in Android Studio**: Import the project folder
2. **Build**: Select Build > Make Project (auto-generates R.java)
3. **Deploy**: Run app on device or emulator (SDK 26+)

## Usage

### Creating a Timer

1. Long-press your home screen and select "Widgets"
2. Find and add "Timer Widget"
3. Enter a name (e.g., "Vinyl Cartridge A")
4. Optionally set initial usage time (leave at 0:00:00 for new items)
5. Tap "Save"

### Using a Timer

- **Start button** — Begin tracking usage time
- **Stop button** — Pause timer (can resume later)
- **Reset button** — Clear usage time back to 0:00:00
- **Edit button** — Change name or manually adjust elapsed time

### Restoring Previous Timers

When you add a new widget instance:
1. If timers exist, they'll appear in a list
2. Select one to restore it on the new widget
3. The accumulated time and state are preserved

## Google Drive Backup and Restore

The app supports:
- Manual backup upload to Google Drive
- Daily scheduled backup upload (when network is available)
- Restoring the latest backup from Drive

Backups are stored in a folder named `timers_backup` in your Google Drive.

### Google Cloud Setup (Required)

1. Create or select a Google Cloud project.
2. Enable **Google Drive API** in that project.
3. Configure OAuth consent screen:
	- Set publishing status to **Testing** for personal/internal use.
	- Add test users who are allowed to sign in.
4. Create an **OAuth 2.0 Client ID** of type **Android**.
5. Use your app package name and signing SHA-1 fingerprint for that client.

If sign-in shows `Sign-in failed: 10`, the package name and SHA-1 in the OAuth Android client usually do not match the installed app build.

### Requesting Access

If you are not already a test user on the OAuth consent screen, request access by email.

- Access requests: **email crappy.crapula8@gmail.com with your Google account address and mention "Timer Widget Drive backup access request" in the subject line**

When prompted by Google for an unverified testing app, choose:
1. **Advanced**
2. **Go to timers (unsafe)**
3. Continue and grant access

## Classes Overview

### TimerData
Handles all persistent storage with SharedPreferences. Manages:
- Global timer registry (all timers ever created)
- Widget-to-timer mapping 
- Elapsed time calculation for running timers
- State persistence across app/device restarts

Key methods:
- `createTimer(name)` — Create new stopwatch at 0:00:00
- `getElapsedMillis(timerId)` — Get current accumulated time (accounts for running state)
- `setTimerRunning(timerId, running)` — Start/stop tracking
- `resetTimer(timerId)` — Set time back to 0:00:00

### TimerWidgetProvider
Main AppWidgetProvider managing widget lifecycle and UI updates.

### TimerService
Background service that:
- Updates widget UI every 100ms when running
- Resumes previously-running timers on app restart
- Stops automatically when no timers are active

### TimerConfigActivity
Configuration UI for creating/editing timers.

## Technical Details

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Java
- **Storage**: SharedPreferences with timer UUIDs
- **Update Interval**: 100ms tick for smooth display
- **State Persistence**: Survives app termination and device reboot

## Use Cases

- **Vinyl vinyl turntables** — Track cartridge runtime before replacement
- **Equipment usage** — Monitor laser cutter/3D printer runtime
- **Device lifetime** — Track bulb hours, battery cycles, etc.
- **Time tracking** — Multiple project or activity timers
- **Maintenance schedules** — Know when service is due based on hours logged
