# AGENTS.md

## Start here

Before changing code, read in this order:

1. `AGENTS.md`
2. `.claude/CURRENT_STATE.md`
3. `.claude/ARCHITECTURE.md`
4. The source, tests, and configuration files directly related to the task.

Read `.claude/archive/` only when a current document links to a historical
decision or the task specifically concerns initial-release behavior.

## Source of truth

1. Current source code and tests.
2. `CURRENT_STATE.md` for a handoff snapshot, unless application source changed
   since its `source_commit`.
3. `ARCHITECTURE.md` for stable constraints.
4. Archived documents for history only.

## Workflow

- Inspect `git status`, `git log`, relevant source, and relevant tests before
  selecting work.
- Preserve uncommitted changes outside the current task. Never use destructive
  Git commands.
- Use TDD for behavior changes when practical: a focused failing test, the
  smallest fix, then a passing focused test.
- After each logical task, run focused tests and `./gradlew :app:assembleDebug`.
- Before reporting a user-visible change complete, run full JVM tests, connected
  tests on an available device, and the applicable manual device path. Record
  unavailable or quarantined coverage rather than claiming it passed.
- Update `.claude/CURRENT_STATE.md` at a meaningful handoff or after a
  user-visible code commit; documentation-only commits do not require an update.

## Git discipline

- Use one English conventional commit per completed logical task.
- Review the staged diff and stage explicit paths only; never use `git add -A`
  in a shared dirty worktree.
- Never push automatically.

## Commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.example.hackernews/.MainActivity
```
