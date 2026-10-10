# MediRappelDZ 💊🇩🇿

Offline-first medication reminder for elderly patients and families in Algeria.
**100% offline — no account, no server, no health data leaves the device.**

## Features
- Medication schedules with exact local alarms (even with screen off)
- Actionable notifications: **Pris ✓ / Sauter** without opening the app
- Intake history + 7-day adherence percentage
- Simple large-text UI (18–20sp base), French + Arabic (RTL)
- Re-schedules automatically after reboot, update, or clock change
- Light / dark via system

## Tech (all clean-room, MIT)
Kotlin · Jetpack Compose Material 3 · Room · AlarmManager (`SCHEDULE_EXACT_ALARM`)
· `POST_NOTIFICATIONS` · minSdk 26 / targetSdk 34

### Exact-alarm justification
The core user-facing function *is* a precisely-timed reminder: a dose at 08:00
must ring at 08:00. The app shows an in-app rationale (Settings screen) before
redirecting to system settings (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), checks
`canScheduleExactAlarms()` before scheduling, re-checks in `onResume`, and
gracefully degrades to an inexact window (`setWindow`) when denied.

## Build
CI (`.github/workflows/android.yml`) runs on every push to `main`:
unit tests → lint → `assembleDebug`, and uploads `app-debug.apk` as artifact.

Locally (Android SDK + JDK 17):
```
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

## Privacy
No network permission. No analytics. No accounts. Room database lives only on
the device. This app is a reminder aid, not a medical device — always follow
your doctor's prescription.
