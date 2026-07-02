---
name: implementation-prepare
description: Prepare git branch for implementation
---

# Implementation Prepare Skill

Prepares git branch and environment for issue implementation.

## Usage

```
/implementation-prepare <issue-number> <commit-type>
```

## Workflow

1. **Fetch main branch**: `git fetch origin && git checkout main && git pull origin main`
2. **Create feature branch**: `git checkout -b <type>-issue-<number>`
3. **Verify branch created**: `git branch --show-current`
4. **Report status**: Show branch name and readiness

## Parameters

- `issue_number` (required): GitHub issue number
- `commit_type` (required): Type of commit (feat, fix, refactor, docs, test)

## Output

- Branch name created: `<type>-issue-<number>`
- Branch status: Active and ready for implementation
- Next step: Begin implementation

## Notes

- Called by orchestrator after validation
- Can be called independently to prepare a branch
- Fails if branch already exists locally
