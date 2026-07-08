# Context

This repo is an Android client for [chores-web](https://github.com/derekwinters/chores-web). Domain terms (Chore, Completion, Assignee, Completer, Credit, Amendment, Reassignment, Points Log, Activity Log, etc.) are defined canonically in [chores-web's CONTEXT.md](https://github.com/derekwinters/chores-web/blob/main/CONTEXT.md) — this repo does not maintain a separate copy, to avoid drift between the two.

Add terms here only when they are specific to this Android client and have no equivalent in chores-web (e.g. UI-only or platform-only concepts).

## Language

**Nav Panel**:
The navigation menu that expands beneath the app header, toggled by the hamburger icon.
_Avoid_: nav drawer, side navigation (the modal drawer pattern it replaced)

**Notification Log**:
The client-side record of notifications this device has received, retained even after a notification is dismissed or goes stale.
_Avoid_: notification history

**Connectivity Alert**:
A client-only notification raised when the app has not successfully reached the server within a configured number of days.
_Avoid_: offline alert, sync warning

## Relationships

- The **Nav Panel** lists top-level destinations only; Settings and Preferences are reached from the avatar menu instead
- The **Notification Log** keeps entries for notifications the server has already dismissed
- The **Connectivity Alert** is raised by the client alone — the server is unaware of it by definition

## Flagged ambiguities

- "Completer" was displayed as the assignee value for chores with no current assignee (issue #162) — resolved: **Completer** is a completion-time concept only; the assignee field shows the current assignee, the next person in rotation, or "Anyone".
- "homepage" / "boards" — resolved: in this app both mean the **Dashboard** destination (the start destination, equivalent to chores-web's `/`).
