---
name: implementation-finalize
description: Push branch to origin and create pull request. Does NOT commit — commits happen at doc-pre and after user approval. Removes in-development label. PR title uses conventional commit format.
---

# Implementation Finalize Skill

Pushes branch to origin and creates the pull request. Commits are made at earlier stages; this skill handles push and PR creation only.

## Usage

```
/implementation-finalize <issue-number> <commit-type>
```

## Workflow

1. **Push branch**: `git push -u origin <branch-name>`
2. **Create PR**:
   - Title: conventional commit format — `<type>: <description> (#<number>)`
   - Body: summary of changes, link to issue, "Closes #<number>" on its own line
3. **Remove `in-development` label**: `gh issue edit <N> --remove-label "in-development"`
4. **Return PR URL**

## Parameters

- `issue_number` (required): GitHub issue number
- `commit_type` (required): Type of commit (feat, fix, refactor, docs, test, chore)

## PR Format

```
Title: <type>: <description> (#<issue-number>)

Body:
## Summary
- <bullet points summarizing changes>

## Issue
Closes #<issue-number>

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## Notes

- Does NOT stage or commit — commits happen at:
  - doc-pre stage: `docs:` commit for documentation updates
  - after user approval: `feat:/fix:/refactor:` commit for code changes
  - doc-validate stage: `docs:` commit if corrections needed (conditional)
- PR title must follow conventional commit format
- "Closes #N" must appear on its own line to trigger GitHub auto-close
- `in-development` label removed at this step (not at commit time)
