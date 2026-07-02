---
name: implementation-verify
description: Verify Android debug build compiles and show changes summary for user review
---

# Implementation Verify Skill

Runs a debug build to verify the app compiles, then shows a summary of changes for user review.

## Usage

```
/implementation-verify <issue-number>
```

## Workflow

1. **Build**: `./gradlew assembleDebug`
2. **Verify build succeeded**: Check exit code, report any compile errors
3. **Prepare changes summary**:
   - List all files modified
   - Show line change counts
   - Summarize implementation
   - Display test results
4. **Pause workflow**: Wait for user approval or request for changes

## Parameters

- `issue_number` (optional): For reference in output

## Output

Shows:
- Files modified with line counts
- Implementation summary
- Test results
- Build status
- Ready for user to:
  - Approve for commit
  - Request more changes
  - Abort

## Notes

- Called by orchestrator after tests pass
- Build verification confirms no compile errors introduced
- Shows all changes before user reviews
- User has control point here
