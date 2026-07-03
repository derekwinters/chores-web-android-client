# Settings sub-navigation for independently-routed sections

## Context

The Settings destination (issue #88) mirrors chores-web's SettingsLayout.jsx, which treats General/Auth/Chores/Theme/Data/About as independently-routed pages, each with its own scroll position and form state. The Android app initially rendered Settings as a single monolithic scrolling form (one long Column with all fields) — this violates the independent routing model and makes it difficult to manage per-section state, scroll position recovery, and incremental field improvements tracked by separate sub-issues.

The fix is to implement sub-navigation within the Settings destination: a menu screen (6 rows) routes to separate screens for each section, each with its own Compose state tree, backed by a shared SettingsViewModel to coordinate the full AppConfig save operation across all sections.

## Considered Options

1. **Single monolithic Settings form** (current): All fields in one Column. Problems: can't recover scroll position when switching sections, hard to manage per-section dirty state, mixing of concerns (General, Auth, Chores, About, Theme, Data).

2. **One ViewModel per section**: Split AppConfig fetch/state across 6 ViewModels. Problems: fetch duplication, stale-data drift if a user edits General, navigates to Auth, then back (their General edits might be stale).

3. **Shared SettingsViewModel scoped to settings nav graph** (chosen): One shared ViewModel instance (via `hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings"))`) fetches and holds the full AppConfig. Each sub-screen shows its section's fields and maintains a local draft; on save, each screen merges its edits into the latest AppConfig and calls the shared `save()`. This matches the web client's pattern (SettingsLayout.jsx coordinates multiple <SettingsFormSection> children).

## Decision

Implement a settings navigation sub-graph with:

- **SettingsMenuScreen**: 6-item menu (General, Auth, Chores, Theme, Data, About) as the start destination of the settings graph.
- **Section screens**: SettingsGeneralScreen, SettingsAuthScreen, SettingsChoresScreen, SettingsAboutScreen (existing ThemeAdminScreen and DataSettingsScreen already exist and are reused).
- **Shared SettingsViewModel**: scoped to the settings nav graph via `hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings"))` in each section. Provides the fetched AppConfig; each section holds its own local draft and Save action.
- **Routes**: `settings/general`, `settings/auth`, `settings/chores`, `settings/about`, plus existing `settings/theme` and `settings/data`.

The backend's `PUT /api/config` endpoint accepts a full AppConfig (not patches), so no network layer changes are required: each section's save merges its local edits into the current AppConfig and calls the existing shared `save()` method.

## Consequences

- **Pro**: Recovers scroll position per section, simplifies per-section state management, aligns with web's model, unblocks per-section improvements (form field restructures, headers, dividers) tracked by separate sub-issues.
- **Pro**: Shared ViewModel prevents stale-data drift; a user's edits to one section are not lost when they navigate to another.
- **Con**: Slightly more boilerplate — each section screen needs its own Composable and test, though they share common patterns (read AppConfig from ViewModel, maintain local draft, merge on save).
