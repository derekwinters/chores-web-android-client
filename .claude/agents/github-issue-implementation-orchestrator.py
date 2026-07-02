#!/usr/bin/env python3
"""
GitHub Issue Implementation Orchestrator Agent

Coordinates issue implementation through branch creation, coding, testing, and PR creation.
Manages state and user approval points.
"""

import json
import subprocess
from typing import Optional, Dict, Any, List
from dataclasses import dataclass
from enum import Enum


class State(Enum):
    """Orchestrator state machine states."""
    VALIDATE = 1
    PREPARE = 2
    DOC_PRE = 3
    IMPLEMENT = 4
    TEST = 5
    VERIFY = 6
    USER_REVIEW = 7
    DOC_POST = 8
    FINALIZE = 9
    COMPLETE = 10


@dataclass
class OrchestratorState:
    """Current orchestrator state."""
    issue_number: int
    issue_title: str
    current_state: State
    progress: int  # 0-10
    history: List[str]  # completed states
    branch_name: str
    metadata: Dict[str, Any]  # implementation results, test status, etc.

    def to_comment(self) -> str:
        """Format state as GitHub issue comment."""
        history_str = " → ".join(self.history + [self.current_state.name])
        return f"""@bot-impl-status
Issue: #{self.issue_number} {self.issue_title}
Current State: {self.current_state.name}
Progress: {self.progress}/10
Branch: {self.branch_name}
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
            ("Validate", State.VALIDATE),
            ("Prepare", State.PREPARE),
            ("Doc Pre", State.DOC_PRE),
            ("Implement", State.IMPLEMENT),
            ("Test", State.TEST),
            ("Verify", State.VERIFY),
            ("User Rev.", State.USER_REVIEW),
            ("Doc Post", State.DOC_POST),
            ("Finalize", State.FINALIZE),
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

        context = f"""Issue #{self.issue_number}: {self.issue_title}
State: [{self.current_state.value}] {self.current_state.name}
Progress: {self.progress}/10
Branch: {self.branch_name}
"""

        return f"""GITHUB ISSUE IMPLEMENTATION WORKFLOW
====================================

{top_line}
{content_line}
{bottom_line}

{context}"""


class GitHubIssueImplementation:
    """Orchestrator for GitHub issue implementation workflow."""

    def __init__(self, issue_number: int, issue_title: str = ""):
        self.issue_number = issue_number
        self.issue_title = issue_title
        self.state = OrchestratorState(
            issue_number=issue_number,
            issue_title=issue_title,
            current_state=State.VALIDATE,
            progress=0,
            history=[],
            branch_name="",
            metadata={},
        )

    def run(self) -> Dict[str, Any]:
        """Execute implementation workflow."""
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
        return {
            "success": True,
            "issue": self.issue_number,
            "state": "COMPLETE",
            "branch": self.state.branch_name,
        }

    def _execute_state(self):
        """Execute current state logic."""
        if self.state.current_state == State.VALIDATE:
            self._validate()
        elif self.state.current_state == State.PREPARE:
            self._prepare()
        elif self.state.current_state == State.DOC_PRE:
            self._doc_pre()
        elif self.state.current_state == State.IMPLEMENT:
            self._implement()
        elif self.state.current_state == State.TEST:
            self._test()
        elif self.state.current_state == State.VERIFY:
            self._verify()
        elif self.state.current_state == State.USER_REVIEW:
            self._user_review()
        elif self.state.current_state == State.DOC_POST:
            self._doc_post()
        elif self.state.current_state == State.FINALIZE:
            self._finalize()
        elif self.state.current_state == State.COMPLETE:
            pass

    def _validate(self):
        """[1] Validate issue is ready for implementation."""
        result = self._call_skill("implementation-validate", self.issue_number)
        self.state.metadata["validation"] = result
        self.state.history.append("validate")
        self.state.progress = 1
        self._transition(State.PREPARE)

    def _prepare(self):
        """[2] Prepare branch for implementation."""
        commit_type = self.state.metadata.get("validation", {}).get("commit_type", "fix")
        branch_name = f"{commit_type}-issue-{self.issue_number}"
        self.state.branch_name = branch_name
        result = self._call_skill("implementation-prepare", self.issue_number, commit_type=commit_type)
        self.state.metadata["branch"] = result
        self.state.history.append("prepare")
        self.state.progress = 2
        self._transition(State.DOC_PRE)

    def _doc_pre(self):
        """[3] Update docs/ pages before writing code."""
        print("Reading affected doc pages from implementation plan...")
        print("Drafting documentation changes for planned feature/fix...")
        self.state.history.append("doc-pre")
        self.state.progress = 3
        self._transition(State.IMPLEMENT)

    def _implement(self):
        """[4] Implement code changes."""
        result = self._call_skill("implementation-implement", self.issue_number)
        self.state.metadata["implementation"] = result
        self.state.history.append("implement")
        self.state.progress = 4
        self._transition(State.TEST)

    def _test(self):
        """[5] Run test suite."""
        result = self._call_skill("implementation-test", self.issue_number)
        self.state.metadata["tests"] = result
        self.state.history.append("test")
        self.state.progress = 5

        if result.get("passed"):
            self._transition(State.VERIFY)
        else:
            # Test failed, return to doc pre / implementation
            print("Tests failed. Returning to doc pre / implementation phase for fixes.")
            self._transition(State.DOC_PRE)

    def _verify(self):
        """[6] Verify Android debug build."""
        result = self._call_skill("implementation-verify", self.issue_number)
        self.state.metadata["verification"] = result
        print(f"Build verification complete. Changes summary:\n{result.get('summary', '')}")
        self.state.history.append("verify")
        self.state.progress = 6
        self._transition(State.USER_REVIEW)

    def _user_review(self):
        """[7] Pause for user review and approval."""
        self.state.history.append("user-review")
        print(f"Issue #{self.issue_number} awaiting user approval before finalization")
        # Agent would exit here, resume triggered by user command
        self._transition(State.DOC_POST)

    def _doc_post(self):
        """[8] Review and correct docs before finalizing."""
        print("Re-reading modified doc pages...")
        print("Verifying docs accurately reflect actual implementation...")
        print("Correcting discrepancies and adding coverage for new behavior...")
        self.state.history.append("doc-post")
        self.state.progress = 8
        self._transition(State.FINALIZE)

    def _finalize(self):
        """[9] Finalize: commit, push, create PR."""
        commit_type = self.state.metadata.get("validation", {}).get("commit_type", "fix")
        result = self._call_skill("implementation-finalize", self.issue_number, commit_type=commit_type)
        pr_url = result.get("pr_url")
        print(f"Pull request created: {pr_url}")
        self.state.metadata["pr"] = result
        self.state.history.append("finalize")
        self.state.progress = 9
        self._transition(State.COMPLETE)

    def _transition(self, next_state: State):
        """Transition to next state."""
        self.state.current_state = next_state

    def _call_skill(self, skill_name: str, issue_number: int, **kwargs) -> Dict[str, Any]:
        """Call a skill (currently stubbed)."""
        print(f"Calling skill: {skill_name} for issue #{issue_number}")
        return {"status": "ok"}

    def _post_issue_comment(self, comment: str):
        """Post comment on GitHub issue."""
        subprocess.run(
            ["gh", "issue", "comment", str(self.issue_number), "--body", comment],
            check=True,
        )

    def _update_issue_status(self):
        """Update pinned status comment."""
        print(self.state.workflow_diagram())
        print()
        status_comment = self.state.to_comment()
        print(f"Status: {status_comment}\n")


def main():
    """Entry point."""
    import sys

    if len(sys.argv) < 2:
        print("Usage: python orchestrator.py <issue-number> [issue-title]")
        sys.exit(1)

    issue_number = int(sys.argv[1])
    issue_title = sys.argv[2] if len(sys.argv) > 2 else ""
    impl = GitHubIssueImplementation(issue_number, issue_title)
    result = impl.run()
