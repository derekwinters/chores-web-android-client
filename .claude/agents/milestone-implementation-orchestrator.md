---
name: milestone-implementation-orchestrator
description: Automated workflow coordinator for implementing all issues in a GitHub milestone end-to-end on a single shared branch and PR
type: agent
---

# Milestone Implementation Orchestrator Agent

Implements all issues in a GitHub milestone sequentially on a shared branch, culminating in a single PR suitable for Release-Please.

## Invocation

```
@milestone-implementation-orchestrator https://github.com/derekwinters/chores-web-android-client/milestone/N
```

## IMPORTANT: Display Workflow Diagram on Every State Transition

Display the workflow diagram each time you transition to a new state, immediately before executing that state's work. Highlight the destination state with heavy borders (┃, ┏┓┗┛).

## State Machine

```
START
  ↓
[1] parse-milestone
  ├─ Extract milestone number from URL
  ├─ gh api fetch milestone title and issue list
  ├─ Extract version string from title (e.g. "v1.9.0" → "1.9.0")
  └─ Result: milestone_number, milestone_title, version, issue_list
          ↓
[2] bulk-validate
  ├─ For each milestone issue: check ready-for-work label + grilling comment + OPEN state
  ├─ Show table of all issues with pass/fail status
  ├─ ABORT if ANY issue fails validation
  └─ Result: validated issue list
          ↓
[3] dependency-order
  ├─ Read all issue titles and grilling comment bodies
  ├─ AI-reason over content to determine safe implementation order
  ├─ Output ordered list with one-line rationale per issue
  └─ Result: implementation_order[]
          ↓
[4] branch-setup
  ├─ git fetch origin && git checkout main && git pull origin main
  ├─ git checkout -b feat/milestone-<version>
  └─ Result: branch feat/milestone-<version> ready
          ↓
[5] release-commit
  ├─ git commit --allow-empty -m "chore: release <version>" with trailer "Release-As: <version>"
  └─ Result: Release-Please anchor commit on branch
          ↓
[6] draft-pr
  ├─ git push -u origin feat/milestone-<version>
  ├─ gh pr create --draft --title "feat: Milestone <version>"
  ├─ Body: ## Milestone <version>\n\n[issue list with titles]\n\n(Closes #N lines appended per-issue during implementation)
  └─ Result: pr_url, pr_number
          ↓
[7] implement-issues (loop over implementation_order)
  ├─ For each issue:
  │   ├─ Invoke github-issue-implementation-orchestrator with:
  │   │   - existing_branch=feat/milestone-<version>
  │   │   - existing_pr=<pr_number>
  │   ├─ Wait for issue orchestrator to complete (do NOT proceed until done)
  │   ├─ On failure: HALT immediately → report failed issue + branch/PR state
  │   └─ On success: gh issue edit <N> --remove-label in-development
  └─ Result: all issues implemented
          ↓
[8] finalize
  ├─ gh pr ready <pr_number>
  └─ Result: PR marked ready for review
          ↓
[9] ci-watch
  ├─ Poll gh pr checks <pr_number> every 30s until all checks are non-pending
  ├─ If all pass → proceed to complete
  ├─ If any fail → diagnose and fix (see CI Fix Loop below)
  └─ Result: all CI checks green
          ↓
[10] complete
  ├─ Display PR URL
  ├─ Info: All milestone issues auto-close when PR merges
  └─ END
```

## Output Format

```
MILESTONE IMPLEMENTATION WORKFLOW
==================================

┌────────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Parse Milestone├─▶│ Bulk Validate ├─▶│  Dep. Order  ├─▶│ Branch Setup ├─▶│ Rel. Commit ├─▶│ Draft PR ├─▶│Impl Loop ├─▶│ Finalize ├─▶│ CI Watch ├─▶│ Complete │
└────────────────┘  └───────────────┘  └──────────────┘  └──────────────┘  └─────────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
```

Also display milestone context at each state:

```
Milestone: v1.9.0 (#7)
State: [9] CI Watch
Progress: 9/10
Branch: feat/milestone-1.9.0
Issues: 8/8 complete
CI: 4/6 passing  (fix attempt 1/3)
```

## Version Extraction

From milestone title, extract semver string:
- "v1.9.0" → "1.9.0"
- "Release 2.0.0" → "2.0.0"
- "Milestone 1.8.0" → "1.8.0"
- "1.8.0" → "1.8.0"

## Bulk Validation Rules

Each milestone issue must have ALL of:
1. State: OPEN
2. Label: `ready-for-work`
3. A grilling comment: any comment body containing `## Grilling Session`

Display results as table:

| Issue | Title | OPEN | ready-for-work | Grilling | Status |
|-------|-------|------|----------------|----------|--------|
| #301  | ...   | ✅   | ✅             | ✅       | PASS   |
| #302  | ...   | ✅   | ❌             | ✅       | FAIL   |

ABORT if any FAIL. Message: "Fix failing issues, then re-run."

## Dependency Ordering

For each issue, read:
- Issue title
- Grilling comment "Impact Areas" table
- Grilling comment "Behaviors to Implement" list

Reason about ordering:
- Database schema changes before code that queries them
- Backend API changes before frontend that calls them
- Shared utility/infrastructure before features that use them
- Independent issues ordered arbitrarily (by issue number)

Output:
```
Implementation order:
1. #301 Add user schema field — database change, must precede API update
2. #303 Update people API — depends on #301 schema
3. #302 Frontend notifications — depends on #303 API
```

## Release-Please Commit

```bash
git commit --allow-empty -m "$(cat <<'EOF'
chore: release <version>

Release-As: <version>
EOF
)"
```

This empty commit signals Release-Please to cut a release at `<version>` when the PR merges.

## Milestone-Mode Context for Issue Orchestrator

Pass to each `github-issue-implementation-orchestrator` invocation:
- `existing_branch=feat/milestone-<version>` — skips branch creation, checks out shared branch
- `existing_pr=<pr_number>` — skips push+PR creation, appends summary + `Closes #N` to existing PR body

The issue orchestrator's `in-development` label removal at finalize is SKIPPED in milestone mode — this orchestrator removes it after each issue completes.

## CI Watch

Invoke the **ci-watch** skill after finalize:

```
/ci-watch <pr_number>
```

The skill polls until all checks resolve and returns a structured `CI_WATCH_RESULT` block.

### On PASSED

Proceed to complete.

### On FAILED

Read the `FAILURES` section from the skill result. Enter the fix loop (max 3 attempts):

1. Diagnose from the `log_excerpt` in each failure
2. Apply targeted fixes to source files
3. Commit: `git commit -m "fix: resolve CI failure in <check-name>"`
4. Push: `git push origin <branch>`
5. Re-invoke `/ci-watch <pr_number>`

On attempt 3 failure: HALT with the full CI_WATCH_RESULT. Do not attempt a 4th fix — require manual intervention.

### What the fix loop may fix autonomously

- Missing env vars in CI workflow steps
- Wrong tool install commands (binary names, asset URLs, paths)
- Test failures caused by code introduced in this milestone
- Gradle build failures due to missing dependency declarations
- API base URL or endpoint mismatches in Retrofit interfaces

### What requires HALT (do not attempt to fix autonomously)

- Flaky/infrastructure failures (runner OOM, network timeouts, GitHub Actions outage)
- Test failures in code predating this milestone (not caused by these changes)
- Security scan failures requiring policy decisions
- Failures in checks not present before this PR

### On TIMEOUT

HALT and report. Do not push fixes — the runner may be degraded.

## Error Handling

| Error | Action |
|-------|--------|
| Milestone URL not found | ABORT |
| Milestone has no issues | ABORT |
| Any issue fails bulk-validate | ABORT — list failing issues |
| Branch already exists | ABORT — re-run safety; delete branch to restart |
| Issue orchestrator fails | HALT — report failed issue, branch, PR URL for manual intervention |
| Push fails | PAUSE — investigate, report |
| CI fix loop exhausted (3 attempts) | HALT — report all failing checks with log excerpts |
| CI poll timeout (40 polls) | HALT — report last known check states |

On issue orchestrator failure, report:
```
HALT: Issue #<N> failed during implementation.
Branch: feat/milestone-<version>
PR: <pr_url>
Completed issues: #X, #Y, #Z
Remaining issues: #A, #B
Manual steps: resolve issue on branch, then re-invoke from #<N>
```

## Notes

- Fully autonomous — no per-issue user review pauses within the milestone loop
- Single shared branch + PR for entire milestone
- Each issue gets its own conventional commit(s) via the issue orchestrator
- PR auto-closes all milestone issues when merged (Closes #N appended per-issue)
- `in-development` label removed per-issue by this orchestrator after each issue completes
- Release-Please reads the empty anchor commit to determine release version
- Safe to re-run from clean state; branch-already-exists check prevents duplicate work

## Related Agents & Skills

### Agents
- **github-issue-implementation-orchestrator**: Implements individual issues; invoked per-issue with milestone-mode params

### Skills Called (in order)
1. *(parse-milestone)* — agent reads GitHub API directly via `gh`
2. *(bulk-validate)* — agent calls `gh` for each issue
3. *(dependency-order)* — agent reasons over issue content
4. *(branch-setup)* — agent runs git commands directly
5. *(release-commit)* — agent runs git commit --allow-empty
6. *(draft-pr)* — agent runs gh pr create --draft
7. **github-issue-implementation-orchestrator** × N — one per issue, with existing_branch + existing_pr
8. *(finalize)* — agent runs gh pr ready
9. **ci-watch** skill — polls gh pr checks, reports pass/fail/timeout; caller handles fixes

## Workflow Chain

1. `github-issue-triage-orchestrator` → labels each issue as `ready-to-grill`
2. `/grill-with-docs issue <N>` × N → labels each as `ready-for-work`
3. `milestone-implementation-orchestrator` → implements all, creates single milestone PR
