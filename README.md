# LifeLog

A production-ready Android application for tracking personal phone activity. LifeLog records app usage, notifications, calls, screen events, battery status, network changes, and optional GPS location — all stored locally on your device.

## Features

- **Dashboard** — Today's screen time, app launches, top apps, notifications, calls, unlocks, and battery info
- **Timeline** — Chronological activity history with icons, colors, and timestamps
- **App Usage** — Daily, weekly, and monthly usage charts via UsageStatsManager
- **Notifications** — Notification history with search and filters
- **Calls** — Incoming, outgoing, and missed call logs
- **Screen Events** — Screen on/off and device unlock tracking
- **Battery & Network** — Battery level, charging state, WiFi, mobile data, Bluetooth, airplane mode
- **Location** — Optional periodic GPS logging (permission required)
- **Global Search** — Search across all logs
- **Export** — CSV, JSON, and PDF export plus database backup
- **Settings** — Theme, language, auto-delete, tracking preferences

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Clean Architecture |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Async | Coroutines, Flow |
| Logging | Timber |
| CI | GitHub Actions (JDK 21) |

## Project Structure

```
app/                    # Application entry point
core/                   # Dispatchers, shared DI
data/                   # Repository implementations, DataStore
domain/                 # Models, repository interfaces, use cases
database/               # Room entities, DAOs
ui/                     # Theme, shared composables
utils/                  # Date/time helpers
service/                # Background tracking services
feature_dashboard/      # Dashboard screen
feature_timeline/       # Timeline screen
feature_apps/           # App usage screen
feature_notifications/  # Notification history
feature_calls/          # Call logs
feature_location/       # Location history
feature_settings/       # Settings & About
feature_permissions/    # Onboarding & permissions
feature_export/         # Global search & export
```

## Requirements

- Android Studio Ladybug or newer
- JDK 21
- Android SDK 35
- Min SDK 26 (Android 8.0)

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Test

```bash
./gradlew test
./gradlew ktlintCheck
```

## Permissions

LifeLog requires several system permissions for full functionality:

| Permission | Purpose |
|------------|---------|
| Usage Access | App usage and screen time tracking |
| Notification Access | Notification history |
| Accessibility | Enhanced screen event detection |
| Location | Optional GPS tracking |
| Phone | Call logging |
| Battery Optimization Exemption | Reliable background tracking |

All permissions are explained during onboarding.

## Privacy

All data is stored locally on your device. Nothing is sent to external servers unless you explicitly export your data.

## License

MIT License — see [LICENSE](LICENSE) for details.
