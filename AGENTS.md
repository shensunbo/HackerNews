# AGENTS.md

## Required Context

Before changing code, read these files in order:

1. `CLAUDE.md`
2. `.claude/spec.md`
3. `.claude/2026-07-15-dev-news-app-design.md`
4. `.claude/2026-07-15-dev-news-app-ui-design.md`
5. `.claude/2026-07-15-dev-news-app-plan.md`
6. `.claude/2026-07-15-dev-news-app-plan-review.md`

The plan review overrides conflicting examples or instructions in the original
implementation plan. Fix applicable P0/P1 findings in existing code before
starting the next numbered task. Apply findings for not-yet-created subsystems
when those subsystems are implemented.

## Determine Progress

- Inspect `git status`, `git log`, existing source, and tests before deciding
  which task is next. Do not rely only on unchecked boxes in the plan.
- Preserve all existing and uncommitted work. Never revert changes that are not
  part of the current task.
- If a completed task conflicts with the review, add a focused regression test
  and correct it before continuing.

## Implementation Workflow

- Continue implementation directly; do not stop after proposing another plan.
- Use TDD for behavior changes: write a failing test, implement the smallest
  correct change, then run the relevant test suite.
- After each logical task, run its focused tests and `./gradlew
  :app:assembleDebug`. Run broader tests when shared behavior changes.
- Diagnose and fix build or test failures. Do not bypass, disable, or silently
  ignore failing verification.
- Keep changes scoped to the current task and follow the existing Kotlin,
  Compose, Room, DataStore, and manual-DI patterns.

## Git And Commands

- Run `./gradlew`, `git add`, and `git commit` directly when needed; configured
  Codex prefix rules are expected to handle approval for these commands.
- Create one English conventional commit for each complete logical task.
- Review the staged diff before committing and do not include unrelated files.
- Never run `git push` automatically.
- Do not use destructive Git commands or remove user changes.

## Completion

- Continue through the remaining tasks until the project is complete or a real
  blocker requires user input.
- Before reporting completion, run the full unit test suite, connected tests on
  the available device, a debug build, and the end-to-end checks defined in the
  implementation plan.
- Report commands run, verification results, remaining risks, and any work that
  could not be verified.
