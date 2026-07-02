---
name: implementation-test
description: Run tests for implemented changes
---

# Implementation Test Skill

Runs the Android test suite to verify implemented changes.

## Usage

```
/implementation-test
```

## Workflow

1. **Run unit tests**: `./gradlew test`
2. **Capture output**: test count, passed/failed, any failures
3. **Check exit code**: 0 = success, non-zero = failure
4. **Report results**:
   - If PASS: All tests passed. Ready for verification.
   - If FAIL: List failed tests and error messages. Blocks workflow.

## Output

- ✅ All tests passed
  - Total tests: N
  - Ready for next phase
- ❌ Tests failed
  - Failed test count and names
  - Error messages
  - Blocks workflow until fixed

## Error Handling

If tests fail:
- Report exactly which tests failed
- Show error output
- Pause workflow for user review and fixes

## Notes

- Called by orchestrator after implementation
- Tests must all pass before proceeding
- Can be called independently to verify changes
- Instrumented tests (`connectedAndroidTest`) require a running emulator/device — unit tests are sufficient for CI gate
