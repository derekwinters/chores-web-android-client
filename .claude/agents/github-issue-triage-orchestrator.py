#!/usr/bin/env python3
"""
GitHub Issue Triage Orchestrator Agent

Coordinates issue validation, categorization, and labeling through a 9-state machine.
Persists state in pinned bot comments on issues.
"""

import json
import subprocess
from typing import Optional, Dict, Any, List
from dataclasses import dataclass
from enum import Enum


class State(Enum):
    """Orchestrator state machine states."""
    CATEGORIZE = 1
    CHECK_DUPLICATES = 2
    VALIDATE = 3
    FEEDBACK = 4
    PAUSE = 5
    REVALIDATE = 6
    APPLY_LABELS = 7
    SUGGEST_MILESTONE = 8
    COMPLETE = 9


@dataclass
class OrchestratorState:
    """Current orchestrator state."""
    issue_number: int
    current_state: State
    progress: int  # 0-9
    history: List[str]  # completed states
    metadata: Dict[str, Any]  # categorize results, validator results, etc.

    def to_comment(self) -> str:
        """Format state as GitHub issue comment."""
        history_str = " → ".join(self.history + [self.current_state.name])
        return f"""@bot-triage-status
Current State: {self.current_state.name}
Progress: {self.progress}/9
History: {history_str}
Next: {self._next_state()}"""

    def _next_state(self) -> str:
        """Get next state after current."""
        try:
            return State(self.current_state.value + 1).name
        except ValueError:
            return "DONE"

    def workflow_diagram(self) -> str:
        """Generate workflow diagram with current state highlighted."""
        states = [
            ("Categorize", State.CATEGORIZE),
            ("Check Dups", State.CHECK_DUPLICATES),
            ("Validate", State.VALIDATE),
            ("Feedback", State.FEEDBACK),
            ("Pause", State.PAUSE),
            ("Re-Valid", State.REVALIDATE),
            ("Labels", State.APPLY_LABELS),
            ("Milestone", State.SUGGEST_MILESTONE),
            ("Complete", State.COMPLETE),
        ]

        # Build top and bottom borders
        top = "┌──────────┐"
        bottom = "└──────────┘"
        current_top = "┏━━━━━━━━━━┓"
        current_bottom = "┗━━━━━━━━━━┛"

        top_line = "  ".join(
            current_top if s[1] == self.current_state else top for s in states
        )
        bottom_line = "  ".join(
            current_bottom if s[1] == self.current_state else bottom for s in states
        )

        # Build content line
        content_parts = []
        for name, state in states:
            if state == self.current_state:
                content_parts.append(f"┃{name:^10}┃")
            else:
                content_parts.append(f"│{name:^10}│")

        content_line = "─▶".join(content_parts)

        return f"""GITHUB ISSUE TRIAGE WORKFLOW
============================

{top_line}
{content_line}
{bottom_line}"""


class GitHubIssueTriage:
    """Orchestrator for GitHub issue triage workflow."""

    def __init__(self, issue_number: int):
        self.issue_number = issue_number
        self.state = OrchestratorState(
            issue_number=issue_number,
            current_state=State.CATEGORIZE,
            progress=0,
            history=[],
            metadata={},
        )

    def run(self) -> Dict[str, Any]:
        """Execute triage workflow."""
        print(self.state.workflow_diagram())
        print()

        while self.state.current_state != State.COMPLETE:
            try:
                self._execute_state()
                self._update_issue_status()
            except Exception as e:
                print(self.state.workflow_diagram())
                print(f"\nError in {self.state.current_state.name}: {e}")
                return {"error": str(e), "state": self.state.current_state.name}

        print(self.state.workflow_diagram())
        return {"success": True, "issue": self.issue_number, "state": "COMPLETE"}

    def _execute_state(self):
        """Execute current state logic."""
        if self.state.current_state == State.CATEGORIZE:
            self._categorize()
        elif self.state.current_state == State.CHECK_DUPLICATES:
            self._check_duplicates()
        elif self.state.current_state == State.VALIDATE:
            self._validate()
        elif self.state.current_state == State.FEEDBACK:
            self._feedback()
        elif self.state.current_state == State.PAUSE:
            self._pause()
        elif self.state.current_state == State.REVALIDATE:
            self._revalidate()
        elif self.state.current_state == State.APPLY_LABELS:
            self._apply_labels()
        elif self.state.current_state == State.SUGGEST_MILESTONE:
            self._suggest_milestone()
        elif self.state.current_state == State.COMPLETE:
            pass

    def _categorize(self):
        """[1] Categorize issue: detect type + effort."""
        # Call github-issue-categorize skill #144
        result = self._call_skill("github-issue-categorize", self.issue_number)
        self.state.metadata["categorize"] = result
        self.state.history.append("categorize")
        self.state.progress = 1
        self._transition(State.CHECK_DUPLICATES)

    def _check_duplicates(self):
        """[2] Check for duplicate/related issues."""
        # Call github-issue-find-duplicates skill #148
        result = self._call_skill("github-issue-find-duplicates", self.issue_number)
        self.state.metadata["duplicates"] = result
        self.state.history.append("check-duplicates")
        self.state.progress = 2
        self._transition(State.VALIDATE)

    def _validate(self):
        """[3] Validate based on issue type."""
        issue_type = self.state.metadata["categorize"].get("type")
        skill_map = {
            "bug": "github-issue-validate-bug",
            "feature": "github-issue-validate-feature",
            "refactor": "github-issue-validate-refactor",
            "chore": "github-issue-validate-bug",  # fallback
        }
        skill = skill_map.get(issue_type, "github-issue-validate-bug")
        result = self._call_skill(skill, self.issue_number)
        self.state.metadata["validation"] = result
        self.state.history.append("validate")
        self.state.progress = 3

        if result.get("valid"):
            # Skip feedback, go to apply labels
            self._transition(State.APPLY_LABELS)
        else:
            # Invalid, post feedback and pause
            self._transition(State.FEEDBACK)

    def _feedback(self):
        """[4] Post feedback comment and pause."""
        # Call github-issue-completeness skill #149
        result = self._call_skill("github-issue-completeness", self.issue_number)
        self._post_issue_comment(result.get("comment", ""))
        self.state.history.append("feedback")
        self.state.progress = 4
        self._transition(State.PAUSE)

    def _pause(self):
        """[5] Pause and wait for user input."""
        # In actual implementation, this would be async/webhook-triggered
        # For now, mark as paused
        self.state.history.append("paused")
        print(f"Issue #{self.issue_number} paused waiting for user input")
        # Agent would exit here, resume triggered by webhook on new comment

    def _revalidate(self):
        """[6] Re-validate after user provides info."""
        # Re-run validation same as [3]
        issue_type = self.state.metadata["categorize"].get("type")
        skill_map = {
            "bug": "github-issue-validate-bug",
            "feature": "github-issue-validate-feature",
            "refactor": "github-issue-validate-refactor",
            "chore": "github-issue-validate-bug",
        }
        skill = skill_map.get(issue_type, "github-issue-validate-bug")
        result = self._call_skill(skill, self.issue_number)
        self.state.metadata["validation"] = result
        self.state.history.append("validate")
        self.state.progress = 6

        if not result.get("valid"):
            # Still invalid, return to feedback
            self._transition(State.FEEDBACK)
        else:
            # Now valid, continue
            self._transition(State.APPLY_LABELS)

    def _apply_labels(self):
        """[7] Apply collected labels."""
        # Collect all target_labels from validation
        target_labels = self.state.metadata.get("validation", {}).get("target_labels", [])
        # Call github-issue-label skill #150
        result = self._call_skill(
            "github-issue-label",
            self.issue_number,
            labels_to_apply=target_labels,
        )
        self.state.metadata["labels"] = result
        self.state.history.append("apply-labels")
        self.state.progress = 7
        self._transition(State.SUGGEST_MILESTONE)

    def _suggest_milestone(self):
        """[8] Suggest milestone."""
        # Call github-issue-suggest-milestone skill #151
        result = self._call_skill("github-issue-suggest-milestone", self.issue_number)
        self.state.metadata["milestone"] = result
        self._post_issue_comment(
            f"**Suggested Milestone**: {result.get('suggested_milestone')} "
            f"({result.get('focus_area')})\n\n{result.get('rationale')}"
        )
        self.state.history.append("suggest-milestone")
        self.state.progress = 8
        self._transition(State.COMPLETE)

    def _transition(self, next_state: State):
        """Transition to next state."""
        self.state.current_state = next_state

    def _call_skill(
        self, skill_name: str, issue_number: int, **kwargs
    ) -> Dict[str, Any]:
        """Call a skill (currently stubbed)."""
        # In real implementation, invoke Claude skill via subprocess or API
        print(f"Calling skill: {skill_name} for issue #{issue_number}")
        return {"status": "ok"}

    def _post_issue_comment(self, comment: str):
        """Post comment on GitHub issue."""
        # In real implementation, use gh CLI or GitHub API
        subprocess.run(
            ["gh", "issue", "comment", str(self.issue_number), "--body", comment],
            check=True,
        )

    def _update_issue_status(self):
        """Update pinned status comment."""
        print(self.state.workflow_diagram())
        print()
        status_comment = self.state.to_comment()
        # In real implementation, find existing status comment and update it
        print(f"Status: {status_comment}\n")


def main():
    """Entry point."""
    import sys

    if len(sys.argv) < 2:
        print("Usage: python orchestrator.py <issue-number>")
        sys.exit(1)

    issue_number = int(sys.argv[1])
    triage = GitHubIssueTriage(issue_number)
    result = triage.run()
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
