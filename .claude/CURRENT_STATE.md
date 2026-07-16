# Current Project State

recorded_at: `2026-07-16T14:10:00+08:00`
source_commit: `f517c5d`
verification_code_base: `f517c5d`

## Current objective

Validate the newly signed Release APK on the device without losing data by
accident.

## Next action

Choose whether to uninstall the currently installed Debug app (which clears its
local data), then install and manually verify the signed Release APK.

## Working tree

The source worktree was clean at `f517c5d`. The local `keystore/readlater-release.jks`
and `keystore.properties` are intentionally ignored and must never be staged.

## Verification matrix

| Scope | Command / check | Result | Code base |
| --- | --- | --- | --- |
| JVM unit tests | `./gradlew :app:testDebugUnitTest` | pass | `af4ddc8` |
| Debug build | `./gradlew :app:assembleDebug` | pass | `af4ddc8` |
| Connected tests | `./gradlew :app:connectedDebugAndroidTest` | pass: 23 tests, 0 failures, 2 skipped | `af4ddc8` on V2324HA / Android 15 |
| Signed Release build | `./gradlew :app:assembleRelease` + `apksigner verify` | pass: v2 signature, one 4096-bit RSA signer | `f517c5d` |
| Device install | Debug APK installed and visually checked; Release install pending | Release: none |

The skipped connected tests are `AppNavTest` and `ProfileScreenIdleTest`; both are
explicitly quarantined for a Compose/Espresso idling-sync hang on this vivo device.

## Known risks

- The phone currently has a Debug-signed app. Installing the same package signed
  by the new Release key requires uninstalling Debug first, which clears its
  local app data.
- The release key is the app's long-term update identity. Back up the ignored
  keystore and `keystore.properties` in a secure password manager or encrypted
  storage; never commit either file.
- RSS failures are aggregated without recording the failing URL and throwable.
- First feed refresh can be slow because RSS feeds are fetched serially.

## Recovery protocol

1. Read `AGENTS.md`, this page, and `.claude/ARCHITECTURE.md`.
2. Run `git status --short --branch`, inspect the relevant source and tests, and
   run the focused verification for the active change.
3. Treat this page as historical when application source has changed since
   `source_commit`; documentation-only commits do not invalidate its code facts.
