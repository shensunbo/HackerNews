# Current Project State

recorded_at: `2026-07-16T14:10:00+08:00`
source_commit: `f517c5d`
verification_code_base: `f517c5d`

## Current objective

Maintain the published signed Release and start the next feature or bugfix from
the verified `v1.0.0` baseline.

## Next action

For a future release, follow `.claude/RELEASE_PROCESS.md`: increment
`versionCode` and `versionName`, rebuild and verify the signed APK, then publish
a new GitHub Release tag.

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
| Device install | `adb install app/build/outputs/apk/release/app-release.apk` | pass: installed and launched on V2324HA / Android 15 |
| GitHub Release | `gh release create v1.0.0` | pass: APK and SHA-256 assets uploaded | `v1.0.0` |

The skipped connected tests are `AppNavTest` and `ProfileScreenIdleTest`; both are
explicitly quarantined for a Compose/Espresso idling-sync hang on this vivo device.

## Known risks

- Release validation required uninstalling the Debug-signed app first, which
  cleared its local app data. Future Release updates signed with this key can
  install over the current Release app and retain its data.
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
