# classics-collector

A **one-time, run-by-hand** JVM tool that rebuilds `app/src/main/assets/classics.json`
— the offline Classics pool — from historical Hacker News + the curated RSS feeds
defined in `topics.json`. It is **not** shipped with the APK and **not** run at
runtime.

## What it does

1. Fetches every RSS feed listed in `topics.json` (keeps each item's title + RSS
   description as the summary).
2. Fetches Hacker News `beststories` (capped at 240 items), keeping stories whose
   title matches a topic's keywords.
3. For items without a summary (HN stories have none), scrapes the page's
   `og:description` / `<meta name="description">` via Jsoup. **No AI rewriting.**
4. Classifies each item into a topic by keyword.
5. Normalizes URLs, de-duplicates by normalized-URL id, and drops items missing a
   title/summary, with an unknown topic, or a non-`http(s)` URL.
6. Self-checks the result: **≥ target** items, unique ids, unique normalized URLs,
   non-blank fields, valid topics, `http(s)` URLs. Aborts (exit 1) on any failure.
7. Writes `classics.json` with a **bumped `poolVersion`** so the runtime
   re-initializes its shuffle state on the next launch.

The pure logic (`normalizeUrl`, `articleIdFor`, `classifyTopic`, `dedupeAndClean`,
`selfCheck`, `nextPoolVersion`) is unit-tested in `CollectorTest`. The network
fetching is not unit-tested (integration only).

## Run

From the repo root (the `run` task uses the repo root as its working directory so
the default asset paths resolve):

```bash
./gradlew :tools:classics-collector:run --args="--target 480"
```

Options:

| flag        | default                                  | meaning                          |
|-------------|------------------------------------------|----------------------------------|
| `--target`  | `480`                                    | minimum/expected pool size       |
| `--topics`  | `app/src/main/assets/topics.json`        | source topics/feeds/keywords     |
| `--out`     | `app/src/main/assets/classics.json`      | destination pool file            |

After running, rebuild & reinstall the app:

```bash
./gradlew :app:installDebug
```

On next launch the runtime detects the new `poolVersion`, generates a fresh
shuffle seed, and starts a new round.

## Note on mirrored helpers

`normalizeUrl` / `articleIdFor` / `stripHtml` / `matchTopics` in `CollectorLogic.kt`
mirror the runtime's `data/remote/ArticleMapper.kt` and `KeywordMatcher.kt` so the
ids the collector emits are identical to the ones the app computes. If those runtime
helpers change, update the mirrors here (or extract a shared module).
