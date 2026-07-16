# Architecture Reference

## Runtime shape

- One Android application module: `app`, package `com.example.hackernews`.
- Jetpack Compose + Material 3 with a fixed terminal-green dark theme; no light
  theme or dynamic color.
- Manual dependency injection starts in `HackerNewsApp` and `di/AppContainer`.
  Do not introduce Hilt without an explicit architecture decision.

## Code map

- `data/config`: bundled `topics.json` and versioned `classics.json` parsing.
- `data/remote`: Hacker News/RSS retrieval and URL normalization.
- `data/local`: Room article/bookmark storage and one Preferences DataStore.
- `data/repository`: feed merge, ranking, and bookmark orchestration.
- `data/classics`: offline-pool selection, persisted batch state, and bookmark
  overlay for Classics.
- `domain/model`: shared article, topic, origin, and reading-mode types.
- `ui`: Compose screens, terminal components, navigation, and link opening.
- `tools/classics-collector`: manually run JVM tool that regenerates the shipped
  Classics asset; it is not packaged into the APK or called at runtime.

## Data invariants

- Article identity is a SHA-256 prefix of the normalized URL; do not add a
  second identity scheme.
- `ArticleOrigin.FEED` and `ArticleOrigin.CLASSIC` stay isolated in feed
  queries. A bookmarked Classic remains available in Bookmarks after its batch
  is replaced.
- Topic preferences, reading mode, and Classics state share the existing
  Preferences DataStore; do not create another store for the `prefs` file.
- Classics is an offline pool. The shipped asset currently has 480 entries at
  `poolVersion: 3`; the app displays deterministic batches of eight and changes
  batch only through the user refresh action. No runtime GitHub, DeepSeek,
  worker, schedule, or network request is involved.
- To deliberately refresh the content pool, run
  `./gradlew :tools:classics-collector:run --args="--target 480"`, review the
  generated `classics.json`, then rebuild and reinstall. The collector preserves
  source titles and uses RSS/page metadata for summaries; it does not rewrite
  content with AI.

## Verification boundaries

- Pure Kotlin behavior belongs in `app/src/test` and runs with
  `./gradlew :app:testDebugUnitTest`.
- Room, DataStore, asset validation, and device behavior belong in
  `app/src/androidTest` and run with
  `./gradlew :app:connectedDebugAndroidTest`.
- `AppNavTest` and `ProfileScreenIdleTest` are intentionally skipped on the
  vivo device due to a Compose/Espresso idling-sync hang. Read `CURRENT_STATE.md`
  before treating connected tests as complete UI coverage.
- A user-visible change needs focused tests when practical, a debug build, and
  the applicable manual device path before completion.
