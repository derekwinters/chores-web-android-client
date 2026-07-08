# Move the avatar to the top bar's nav-icon slot; make actions per-screen

## Context

Issue #177 tried fixing a bug where the chore list's floating Add-Chore button could permanently overlap a fully-visible, unscrolled row's action icons (confirmed via a semantics dump: on a narrow viewport, a due chore's last action icon and the FAB landed at identical coordinates, with no scroll affordance to reveal the overlap since nothing appeared cut off). Multiple attempts at reserving clearance around the FAB (container padding, `LazyColumn` `contentPadding`, forced scroll-to-index) either reintroduced the original dead-space complaint or failed to actually clear the icon, because the FAB and the list share the same screen region by construction — any fix was going to be a workaround, not a resolution.

The simpler fix is to not have a FAB on this screen at all. That freed up the top app bar as a place to put Add Chore instead, which raised a second question: the top bar's `actions` slot already held the user avatar (added under issue #59 to restore web's identity treatment), and Material's convention reserves the left `navigationIcon` slot for back/nav icons, not identity. Since this app's bottom-nav structure (ADR-0004) has no back stack needing that slot, it was open.

## Decision

The avatar moves from `actions` (right) to `navigationIcon` (left) in `ChoresAuthenticatedScaffold`, shown as the colored initial circle only (the inline username text is dropped — the nav-icon slot is sized for a single compact icon, not icon+text). The username is preserved as a non-clickable "Signed in as {username}" header at the top of the same dropdown the avatar already opens.

The `actions` slot becomes screen-aware: rather than always rendering the same fixed content, it now conditionally renders per the current destination. Its first use is an icon-only "Add Chore" button, shown only while on the chores list route specifically (an exact match, not the existing prefix-based `isCurrent()` helper used for bottom-nav highlighting, since `"chores/new"` and `"chores/{choreId}/edit"` also start with `"chores"` and would otherwise show the button on those screens too).

## Consequences

- **Pro**: Removes the FAB/list-overlap bug at its source instead of continuing to patch around it.
- **Pro**: Establishes a reusable pattern — any screen can now contribute a top-bar action conditionally, rather than the bar being one fixed global thing. The already-discussed idea of moving the chore list's Filters button to the top bar next would follow this same pattern.
- **Con**: Deviates from Material's left=nav/right=actions convention (avatar isn't a nav/back affordance). Deliberate, given this app has no back-stack use for that slot.
- **Con**: Per-screen `actions` content needs an exact-route check per screen going forward, not the loose prefix match already in use elsewhere in this file for tab highlighting — a future screen reusing `isCurrent()` for this purpose would inherit the same false-positive risk on its own sub-routes.
