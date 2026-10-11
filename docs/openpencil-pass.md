# OpenPencil pass — medirappel-dz

## Design (Shared/OpenPencil/medirappel-dz/)
- `design.html` (large-type dose card + 56dp action button + empty state, DZ tokens)
  → `design.fig` via `openpencil import`
- `lint --preset accessibility`: 0 errors, 0 warnings

## Code changes
- NEW `ui/theme/DzStates.kt`: `DzSectionTitle` + `DzEmptyState` in the app's large-type scale
  (additive only — `MainActivity`/`MediRappelTheme` untouched, they already do edge-to-edge +
  dynamic color + large type).

## APKs
`android.yml` now also runs `:app:assembleRelease` (unsigned) and uploads
`medirappeldz-release-apk` alongside `medirappeldz-debug-apk`. Both per workflow run in Artifacts.
