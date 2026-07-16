# Agent-Friendly Documentation Implementation Plan

> **For agentic workers:** Execute this plan in the current workspace with small, reviewable documentation-only commits. Do not touch product source files or include unrelated dirty work.

**Goal:** Replace the initial-release document chain with a small, tool-neutral documentation system that lets a future agent safely find current state, architecture, constraints, and verification requirements before adding a feature or fixing a bug.

**Architecture:** `AGENTS.md` becomes the single operational entry point; `CLAUDE.md` becomes a short compatibility redirect. `.claude/CURRENT_STATE.md` is the only mutable recovery document, while `.claude/ARCHITECTURE.md` contains stable implementation facts. All initial-release source documents remain available under a dated archive, but are no longer default reading.

**Tech Stack:** Markdown, Git history, Gradle/ADB verification commands; no application-code, Gradle, or resource changes.

---

## Safety and source-of-truth rules

- Preserve every pre-existing uncommitted file. At plan creation time `app/src/main/java/com/example/hackernews/di/AppContainer.kt` is dirty; documentation work must not stage or modify it.
- Do not use `git add -A`. Stage each documentation path explicitly.
- Before writing `CURRENT_STATE.md`, inspect current `git status --short --branch`, `git log -8 --oneline --decorate`, and the latest test/build reports. Do not copy claims from the old execution log unless their `verified_commit` equals the current commit.
- Use this authority order in every new document: current source and tests, then `CURRENT_STATE.md` when its `verified_commit` matches `HEAD`, then `ARCHITECTURE.md`, then archived material.

## Target layout

```text
AGENTS.md
CLAUDE.md
README.md

.claude/
  CURRENT_STATE.md
  ARCHITECTURE.md
  templates/
    feature-plan.md
    bugfix.md
  archive/
    initial-release-2026-07-15/
      README.md
      spec.md
      design.md
      ui-design.md
      implementation-plan.md
      plan-review.md
      execution-status.md
```

### Task 1: Establish the current-state contract and create the recovery page

**Files:**

- Create: `.claude/CURRENT_STATE.md`
- Modify: none
- Verify: the document's `recorded_head` equals `git rev-parse --short HEAD` at the moment it is written.

- [ ] **Step 1: Capture the factual snapshot before writing**

Run:

```bash
git status --short --branch
git log -8 --oneline --decorate
git rev-parse --short HEAD
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
adb devices -l
```

Record only commands that completed successfully. If a command fails, record the failure, the command, and its blocking condition; do not mark the corresponding verification as passing.

- [ ] **Step 2: Write the failing documentation check**

Run the following checks before the file exists:

```bash
test -f .claude/CURRENT_STATE.md
rg -n '^recorded_head: |^verified_commit: |^## Current objective$|^## Next action$|^## Verification matrix$|^## Known risks$' .claude/CURRENT_STATE.md
```

Expected: the first command fails because the recovery document does not exist.

- [ ] **Step 3: Create the one-page status document with this exact structure**

```markdown
# Current Project State

recorded_at: `<ISO-8601 time written>`
recorded_head: `<output of git rev-parse --short HEAD>`
verified_commit: `<commit covered by the newest complete verification, or none>`

## Current objective

One sentence describing the only active feature, bug, or verification task. If the worktree is dirty, name the affected area and state that no completion claim applies until it is verified.

## Next action

One concrete next action. It must be executable without reopening the initial-release plan.

## Working tree

Paste the current `git status --short --branch` output in a fenced text block. State which paths belong to the active task and that all other dirty paths must be preserved.

## Verification matrix

| Scope | Command | Result | Covered commit |
| --- | --- | --- | --- |
| JVM unit tests | exact command or `not run` | pass/fail/not run | commit or `none` |
| Debug build | exact command or `not run` | pass/fail/not run | commit or `none` |
| Connected tests | exact command or `not run` | pass/fail/not run | commit or `none` |
| Device checks | exact command/checklist or `not run` | pass/fail/not run | commit or `none` |

## Known risks

- RSS source failures are aggregated but are not yet logged with the failing URL and throwable.
- Two Compose instrumented test classes are intentionally skipped on the vivo device because of idling synchronization hangs; they do not count as verified UI coverage.
- First feed refresh can be slow because RSS sources are fetched serially.

## Recovery protocol

1. Read `AGENTS.md`, this file, and `.claude/ARCHITECTURE.md`.
2. Run `git status --short --branch`, `git log -8 --oneline --decorate`, and the focused test for the active task.
3. Treat every verification row whose covered commit differs from `HEAD` as historical evidence only.
```

Replace only the backticked factual fields with the output from Step 1. Keep the status page below roughly 120 lines; detailed old evidence belongs in the archive.

- [ ] **Step 4: Verify the new contract**

Run:

```bash
test -f .claude/CURRENT_STATE.md
rg -n '^recorded_head: |^verified_commit: |^## Current objective$|^## Next action$|^## Verification matrix$|^## Known risks$' .claude/CURRENT_STATE.md
git rev-parse --short HEAD
```

Expected: all required headings exist and the value after `recorded_head:` matches the final command.

- [ ] **Step 5: Commit only the status document**

```bash
git add .claude/CURRENT_STATE.md
git diff --cached --check
git diff --cached -- .claude/CURRENT_STATE.md
git commit -m "docs: add concise current project state"
```

### Task 2: Create the stable architecture reference and reusable work templates

**Files:**

- Create: `.claude/ARCHITECTURE.md`
- Create: `.claude/templates/feature-plan.md`
- Create: `.claude/templates/bugfix.md`
- Verify: all documented paths exist and no archived document is required for an ordinary change.

- [ ] **Step 1: Write the failing documentation checks**

Run:

```bash
test -f .claude/ARCHITECTURE.md
test -f .claude/templates/feature-plan.md
test -f .claude/templates/bugfix.md
```

Expected: all three checks fail because the files do not exist.

- [ ] **Step 2: Create `.claude/ARCHITECTURE.md` with these sections and facts**

```markdown
# Architecture Reference

## Runtime shape

- One Android application module, package `com.example.hackernews`.
- Jetpack Compose + Material 3 UI; fixed terminal-green dark theme; no dynamic color or light theme.
- Manual dependency injection begins in `HackerNewsApp` and `di/AppContainer`; do not introduce Hilt without an explicit architectural decision.

## Code map

- `data/config`: bundled `topics.json` and `classics.json` parsing.
- `data/remote`: Hacker News and RSS retrieval plus URL normalization.
- `data/local`: Room article/bookmark storage and Preferences DataStore.
- `data/repository`: feed merge/rank/bookmark orchestration; `data/classics` owns the Classics pool selection and persisted progress.
- `domain/model`: shared article, topic, reading-mode, and origin types.
- `ui`: Compose screens, reusable terminal components, navigation, and link opening.

## Data invariants

- Article identity is the normalized URL SHA-256 prefix; never create a second identity scheme for the same article.
- `ArticleOrigin.FEED` and `ArticleOrigin.CLASSIC` remain isolated in the feed query; bookmarked Classics remain visible through Bookmarks after leaving the current Classics batch.
- Topic preferences and reading mode are stored in one Preferences DataStore instance; do not declare a second store for the same `prefs` file.
- Classics is an offline pool: no GitHub, DeepSeek, background worker, scheduled refresh, or runtime network request. Its batch only changes through the explicit user refresh action.

## Verification boundaries

- Pure Kotlin behavior belongs in JVM tests under `app/src/test`.
- Room/DataStore/Compose-device behavior belongs in `app/src/androidTest`; read the skip status in `CURRENT_STATE.md` before treating connected tests as complete UI coverage.
- Feature completion requires focused tests, `:app:assembleDebug`, full JVM tests, connected tests on the available device, and the applicable manual device path.
```

- [ ] **Step 3: Create the exact Feature template**

```markdown
# <Feature name>

## User outcome

One observable user result.

## Scope

- In: explicit user-visible behavior and affected data.
- Out: behavior deliberately not changed.

## Design decisions

- Data ownership:
- UI entry point:
- Failure behavior:
- Compatibility or migration:

## Implementation checklist

1. Add a focused failing test for the first behavior.
2. Implement the smallest change and run the focused test.
3. Repeat for each behavior.
4. Run `./gradlew :app:assembleDebug`.
5. Update `CURRENT_STATE.md` with the exact commit and verification evidence.

## Acceptance checks

- Exact JVM or connected command:
- Exact device interaction:
- Expected failure/offline behavior:
```

- [ ] **Step 4: Create the exact Bugfix template**

```markdown
# <Bug title>

## Symptom

Describe the user-visible failure and the affected build/device.

## Reproduction

1. Exact initial state.
2. Exact user action.
3. Expected result versus actual result.

## Root cause

Name the responsible data flow or UI boundary after investigation.

## Regression test

State the exact test class and assertion that fails before the fix and passes after it.

## Fix and verification

- Smallest code change:
- Focused test command:
- Required build/device command:
- `CURRENT_STATE.md` update required: yes.
```

- [ ] **Step 5: Verify the stable reference and templates**

Run:

```bash
rg -n 'data/classics|ArticleOrigin.CLASSIC|Preferences DataStore|:app:assembleDebug' .claude/ARCHITECTURE.md
rg -n 'User outcome|Implementation checklist|Acceptance checks' .claude/templates/feature-plan.md
rg -n 'Symptom|Reproduction|Regression test|Fix and verification' .claude/templates/bugfix.md
```

Expected: each command reports the exact headings and architecture invariants above.

- [ ] **Step 6: Commit the stable reference and templates**

```bash
git add .claude/ARCHITECTURE.md .claude/templates/feature-plan.md .claude/templates/bugfix.md
git diff --cached --check
git diff --cached -- .claude/ARCHITECTURE.md .claude/templates/feature-plan.md .claude/templates/bugfix.md
git commit -m "docs: add architecture reference and agent templates"
```

### Task 3: Make `AGENTS.md` the operational authority and minimize `CLAUDE.md`

**Files:**

- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Verify: an ordinary feature or bug requires only `AGENTS.md`, `CURRENT_STATE.md`, `ARCHITECTURE.md`, and one relevant source area.

- [ ] **Step 1: Write the failing routing checks**

Run:

```bash
rg -n '2026-07-15-dev-news-app-plan.md|Required Context' AGENTS.md
rg -n '22 个 TDD task|executing-plans|subagent-driven-development' CLAUDE.md
```

Expected: the old mandatory initial-release routing is present, proving that the entry documents still need replacement.

- [ ] **Step 2: Replace `AGENTS.md` with this operational structure**

```markdown
# AGENTS.md

## Start here

Before changing code, read in this order:

1. `AGENTS.md`
2. `.claude/CURRENT_STATE.md`
3. `.claude/ARCHITECTURE.md`
4. The source, tests, and configuration files directly related to the task.

Read `.claude/archive/` only when a current document links to a historical decision or a task explicitly concerns initial-release behavior.

## Source of truth

1. Current source code and tests.
2. `CURRENT_STATE.md` only when `verified_commit` matches `HEAD`.
3. `ARCHITECTURE.md` for stable constraints.
4. Archived documents for history only.

## Workflow

- Inspect `git status`, `git log`, relevant source, and relevant tests before selecting work.
- Preserve all uncommitted changes that are outside the current task. Never use destructive Git commands.
- Use TDD for behavior changes: failing focused test, smallest fix, passing focused test.
- After each logical task run its focused tests and `./gradlew :app:assembleDebug`.
- Before reporting completion run full JVM tests, connected tests on the available device, a debug build, and the applicable manual path.
- Update `.claude/CURRENT_STATE.md` after every logical commit with the active goal, next action, dirty-worktree summary, and verification evidence.

## Git discipline

- Use one English conventional commit per completed logical task.
- Review the staged diff and stage explicit paths only; never use `git add -A` in a shared dirty worktree.
- Never push automatically.

## Commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.example.hackernews/.MainActivity
```
```

- [ ] **Step 3: Replace `CLAUDE.md` with this compatibility page**

```markdown
# ReadLater

Personal Android development-news reader for vivo X100S Pro (Android 15). The app is a pure client: Hacker News + RSS feed, offline Classics pool, bookmarks, topic preferences, and Custom Tabs/external-browser reading.

## Agent instructions

Read and follow [`AGENTS.md`](AGENTS.md). It is the single operational authority for all agents and tools.

## Quick commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Current state and architecture are in `.claude/CURRENT_STATE.md` and `.claude/ARCHITECTURE.md`.
```

- [ ] **Step 4: Verify the routing change**

Run:

```bash
rg -n 'CURRENT_STATE.md|ARCHITECTURE.md|Source of truth|stage explicit paths only' AGENTS.md
rg -n 'single operational authority|AGENTS.md|CURRENT_STATE.md' CLAUDE.md
rg -n '2026-07-15-dev-news-app-plan.md|22 个 TDD task' AGENTS.md CLAUDE.md
```

Expected: the first two commands find the new routing rules; the last command returns no matches.

- [ ] **Step 5: Commit the entry-document migration**

```bash
git add AGENTS.md CLAUDE.md
git diff --cached --check
git diff --cached -- AGENTS.md CLAUDE.md
git commit -m "docs: make agents guide the canonical entry point"
```

### Task 4: Archive initial-release documents and add a human-facing README

**Files:**

- Create: `README.md`
- Create: `.claude/archive/initial-release-2026-07-15/README.md`
- Move: `.claude/spec.md`
- Move: `.claude/2026-07-15-dev-news-app-design.md`
- Move: `.claude/2026-07-15-dev-news-app-ui-design.md`
- Move: `.claude/2026-07-15-dev-news-app-plan.md`
- Move: `.claude/2026-07-15-dev-news-app-plan-review.md`
- Move: `.claude/2026-07-15-dev-news-app-execution-status.md`

- [ ] **Step 1: Write the failing documentation checks**

Run:

```bash
test -f README.md
test -f .claude/archive/initial-release-2026-07-15/README.md
```

Expected: both checks fail because the new README files do not exist.

- [ ] **Step 2: Create the root README with this content**

```markdown
# ReadLater

Personal Android reader for software-development news and evergreen engineering articles.

## What it does

- Aggregates Hacker News and selected RSS feeds.
- Lets users tune topic preferences and bookmark articles.
- Opens originals through Custom Tabs or the external browser.
- Provides an offline Classics pool that changes only when the user requests the next batch.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.example.hackernews/.MainActivity
```

## Development documentation

Agent workflow: [`AGENTS.md`](AGENTS.md)

Current work and verification: [`.claude/CURRENT_STATE.md`](.claude/CURRENT_STATE.md)

Architecture: [`.claude/ARCHITECTURE.md`](.claude/ARCHITECTURE.md)
```

- [ ] **Step 3: Move the initial-release files with Git and create the archive index**

Run:

```bash
mkdir -p .claude/archive/initial-release-2026-07-15
git mv .claude/spec.md .claude/archive/initial-release-2026-07-15/spec.md
git mv .claude/2026-07-15-dev-news-app-design.md .claude/archive/initial-release-2026-07-15/design.md
git mv .claude/2026-07-15-dev-news-app-ui-design.md .claude/archive/initial-release-2026-07-15/ui-design.md
git mv .claude/2026-07-15-dev-news-app-plan.md .claude/archive/initial-release-2026-07-15/implementation-plan.md
git mv .claude/2026-07-15-dev-news-app-plan-review.md .claude/archive/initial-release-2026-07-15/plan-review.md
git mv .claude/2026-07-15-dev-news-app-execution-status.md .claude/archive/initial-release-2026-07-15/execution-status.md
```

Then create `.claude/archive/initial-release-2026-07-15/README.md`:

```markdown
# Initial-release archive

These files describe the 2026-07-15 initial implementation effort. They are retained for Git history and design archaeology, not as default instructions for current work.

| File | Use it when |
| --- | --- |
| `spec.md` | Investigating the original product request. |
| `design.md` | Tracing an intentional original product decision. |
| `ui-design.md` | Comparing a UI change against the original terminal visual system. |
| `implementation-plan.md` | Looking up original task sequencing or commands. |
| `plan-review.md` | Checking why a P0/P1/P2 correction exists. |
| `execution-status.md` | Auditing historical verification evidence for the recorded old commit only. |

For active work, use `../../CURRENT_STATE.md` and `../../ARCHITECTURE.md` instead.
```

- [ ] **Step 4: Verify archive completeness and active routing**

Run:

```bash
rg --files .claude/archive/initial-release-2026-07-15 | sort
test ! -e .claude/spec.md
test ! -e .claude/2026-07-15-dev-news-app-plan.md
rg -n 'CURRENT_STATE.md|ARCHITECTURE.md|AGENTS.md' README.md .claude/archive/initial-release-2026-07-15/README.md
```

Expected: the archive contains seven files including its README; the old root-level initial-release paths are absent; both indexes point to the new active documents.

- [ ] **Step 5: Commit archive and README separately**

```bash
git add README.md .claude/archive
git diff --cached --check
git diff --cached -- README.md .claude/archive
git commit -m "docs: archive initial release documents"
```

### Task 5: Run the documentation acceptance pass

**Files:**

- Verify: `AGENTS.md`, `CLAUDE.md`, `README.md`, `.claude/CURRENT_STATE.md`, `.claude/ARCHITECTURE.md`, `.claude/templates/`, and `.claude/archive/`.

- [ ] **Step 1: Check the default reading path**

Run:

```bash
for path in AGENTS.md .claude/CURRENT_STATE.md .claude/ARCHITECTURE.md; do test -f "$path"; done
rg -n '^1\. `AGENTS.md`$|^2\. `\.claude/CURRENT_STATE.md`$|^3\. `\.claude/ARCHITECTURE.md`$' AGENTS.md
```

Expected: all files exist and `AGENTS.md` lists exactly the three default documents in that order.

- [ ] **Step 2: Check link targets and stale initial-release claims**

Run:

```bash
rg -n '\]\((AGENTS\.md|\.claude/CURRENT_STATE\.md|\.claude/ARCHITECTURE\.md)\)' README.md CLAUDE.md
rg -n 'Task 22.*当前|Classics.*8 条|22 个 TDD task|待生成实现计划' AGENTS.md CLAUDE.md README.md .claude/CURRENT_STATE.md .claude/ARCHITECTURE.md
```

Expected: the first command finds active-document links. The second command returns no matches; any such statement belongs only in the archive.

- [ ] **Step 3: Check Markdown and repository hygiene**

Run:

```bash
git diff --check
git status --short
git diff --name-only
```

Expected: no whitespace errors. The diff contains only the explicit documentation paths from this plan plus any pre-existing user work; no Kotlin, Gradle, resource, or asset path is staged by the documentation task.

- [ ] **Step 4: Perform a fresh-agent dry run**

Use the following prompt in a fresh agent context:

```text
Read the project instructions and explain: (1) the active task, (2) the next safe action,
(3) where the Classics pool behavior is defined, (4) which verification evidence applies to HEAD,
and (5) which historical file to inspect for original UI intent.
```

Expected answer: it cites `AGENTS.md`, `CURRENT_STATE.md`, `ARCHITECTURE.md`, current source/tests, and the archived `ui-design.md`; it does not treat the archived execution log as current verification.

- [ ] **Step 5: Commit the documentation acceptance record**

If Tasks 1–4 were committed separately and no additional documentation changes were needed, do not create an empty commit. Otherwise stage only the corrected documentation paths and use:

```bash
git add AGENTS.md CLAUDE.md README.md .claude/CURRENT_STATE.md .claude/ARCHITECTURE.md .claude/templates .claude/archive
git diff --cached --check
git commit -m "docs: verify agent documentation workflow"
```

## Plan self-review

- The plan replaces the six-document mandatory chain with three current documents, a short compatibility page, and one archive index.
- It preserves original requirements, design rationale, review findings, and historical test evidence through `git mv` instead of deletion.
- It makes stale verification detectable by binding every state claim to `verified_commit`.
- It keeps mutable state, stable architecture, task templates, and archived history in separate locations so future feature and bug work does not append noise to one file.
- Every modification is documentation-only and every commit uses explicit paths, so active application-code work remains untouched.
