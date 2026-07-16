# Current Project State

recorded_at: `2026-07-16T13:34:19+08:00`
source_commit: `a3c5f9a`
verification_code_base: `a3c5f9a + uncommitted Classics app-bar UI change`

## Current objective

Finish the documentation handoff after the Classics pool refresh; keep the pending
Classics app-bar cleanup separate from documentation commits.

## Next action

Review and commit the two pending Classics UI paths; the automated checks and
manual device visual check have been completed.

## Working tree

```text
## main
 M app/src/main/java/com/example/hackernews/ui/classics/ClassicsScreen.kt
?? app/src/main/res/drawable/
```

Both paths are one active UI change: remove the batch-progress label and replace
the textual refresh action with a refresh icon. Preserve them while working on
documentation.

## Verification matrix

| Scope | Command / check | Result | Code base |
| --- | --- | --- | --- |
| JVM unit tests | `./gradlew :app:testDebugUnitTest` | pass | current worktree |
| Debug build | `./gradlew :app:assembleDebug` | pass | current worktree |
| Connected tests | `./gradlew :app:connectedDebugAndroidTest` | pass: 23 tests, 0 failures, 2 skipped | current worktree on V2324HA / Android 15 |
| Device install | `./gradlew :app:installDebug` | pass earlier on 2026-07-16 | current worktree |
| Manual Classics app-bar check | installed app on V2324HA | pass: Feed, Classics, and Profile screenshots captured; no batch text and refresh icon visible | current worktree |

The skipped connected tests are `AppNavTest` and `ProfileScreenIdleTest`; both are
explicitly quarantined for a Compose/Espresso idling-sync hang on this vivo device.

## Known risks

- The pending Classics app-bar change is verified but still uncommitted; do not
  mix it with unrelated work.
- RSS failures are aggregated without recording the failing URL and throwable.
- First feed refresh can be slow because RSS feeds are fetched serially.

## Recovery protocol

1. Read `AGENTS.md`, this page, and `.claude/ARCHITECTURE.md`.
2. Run `git status --short --branch`, inspect the relevant source and tests, and
   run the focused verification for the active change.
3. Treat this page as historical when application source has changed since
   `source_commit`; documentation-only commits do not invalidate its code facts.
