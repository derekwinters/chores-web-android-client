package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.ui.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/
 * points/state/next_due" and "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null". Issue #13 adds live search, filters, and sorting; issue #14 adds
 * the stats panel above this (see ChoresStatsPanel); issue #12 adds [initialAssignee]/
 * [initialDueWithin] so Dashboard's Due Now/Due Soon links land here pre-filtered; issue #15
 * adds tap-to-expand row detail plus Skip/Mark Due Now/Delete/History actions; issue #16 adds
 * the Add-chore FAB and per-row Edit action via [navActions].
 *
 * Thin Hilt-wired wrapper around [ChoreListContent]; see ChoreListContentTest for behavior
 * coverage that doesn't require a Hilt test component.
 */
@Composable
fun ChoreListScreen(
    modifier: Modifier = Modifier,
    initialAssignee: String? = null,
    initialDueWithin: String? = null,
    navActions: ChoresNavActions = ChoresNavActions(),
    viewModel: ChoreListViewModel = hiltViewModel()
) {
    val visibleState by viewModel.visibleChores.collectAsState()
    val allChoresState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val completingChoreId by viewModel.completingChoreId.collectAsState()
    val pendingActionChoreId by viewModel.pendingActionChoreId.collectAsState()
    val availablePeople by viewModel.availablePeople.collectAsState()

    LaunchedEffect(initialAssignee, initialDueWithin) {
        if (initialAssignee != null || initialDueWithin != null) {
            val dueWithin = initialDueWithin?.let { name -> runCatching { DueWithinFilter.valueOf(name) }.getOrNull() }
            viewModel.applyInitialFilters(initialAssignee, dueWithin)
        }
    }

    val allChores = (allChoresState as? UiState.Success)?.data ?: emptyList()

    ChoreListContent(
        modifier = modifier,
        statsPanel = { ChoresStatsPanel() },
        uiState = visibleState,
        totalCount = allChores.size,
        filters = filters,
        availableAssignees = allChores.availableAssigneeOptions(),
        availableScheduleTypes = allChores.availableScheduleTypes(),
        availableAssignmentTypes = allChores.availableAssignmentTypes(),
        availablePeople = availablePeople,
        completingChoreId = completingChoreId,
        pendingActionChoreId = pendingActionChoreId,
        onComplete = viewModel::completeChore,
        onSkip = viewModel::skipChore,
        onMarkDue = viewModel::markChoreDue,
        onDelete = viewModel::deleteChore,
        onHistory = { chore -> navActions.onNavigateToHistory(chore.name) },
        onEdit = { chore -> navActions.onNavigateToEditChore(chore.id) },
        onAddChore = navActions.onNavigateToCreateChore,
        onQueryChange = viewModel::updateQuery,
        onFiltersChange = viewModel::updateFilters
    )
}

@Composable
fun ChoreListContent(
    uiState: UiState<List<Chore>>,
    completingChoreId: Int?,
    onComplete: (Chore, String?) -> Unit,
    modifier: Modifier = Modifier,
    totalCount: Int = (uiState as? UiState.Success)?.data?.size ?: 0,
    filters: ChoreFilters = ChoreFilters(),
    availableAssignees: List<String> = emptyList(),
    availableScheduleTypes: List<String> = emptyList(),
    availableAssignmentTypes: List<String> = emptyList(),
    availablePeople: List<String> = emptyList(),
    pendingActionChoreId: Int? = null,
    onSkip: (Chore) -> Unit = {},
    onMarkDue: (Chore) -> Unit = {},
    onDelete: (Chore) -> Unit = {},
    onHistory: (Chore) -> Unit = {},
    onEdit: (Chore) -> Unit = {},
    onAddChore: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onFiltersChange: (ChoreFilters) -> Unit = {},
    statsPanel: @Composable () -> Unit = {}
) {
    var choreAwaitingCompleter by remember { mutableStateOf<Chore?>(null) }
    // Issue #74 CI fix: hoisted up from ChoreRow (a LazyColumn item composable) to match every
    // other delete-confirmation dialog in this codebase (PointsLog, ThemeAdmin) and this same
    // file's own CompleterPickerDialog below -- all of which keep their "awaiting confirmation"
    // state as a sibling of the list, not owned by a list item itself. Owning it inside the
    // LazyColumn item was the actual root cause of choreListContent_deleteAction_requiresConfirmation
    // intermittently failing to find the dialog after the list's layout changed (issue #74):
    // list-item-owned state is subject to that item's own composition lifecycle, which isn't
    // guaranteed stable in the same way a parent-level remember is.
    var choreAwaitingDelete by remember { mutableStateOf<Chore?>(null) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            statsPanel()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = filters.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_chores_label)) },
                singleLine = true,
                // Issue #69: leading search icon + trailing clear ("x") button, matching web's
                // search field.
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                }
            )
            // Issue #72: visible text label alongside the filter toggle's icon (previously
            // icon-only), matching web's affordance.
            TextButton(onClick = { showFiltersDialog = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = null)
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(R.string.filters_title)
                )
            }
        }

        // Issue #74: "Showing N of M chores" is now always visible (previously hidden unless
        // filters were active), matching web's always-visible count. "Clear filters" remains
        // conditional -- it wouldn't make sense to offer clearing filters that aren't active.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val visibleCount = (uiState as? UiState.Success)?.data?.size ?: 0
            Text(
                text = stringResource(R.string.showing_n_of_m_chores, visibleCount, totalCount),
                style = MaterialTheme.typography.bodySmall
            )
            if (filters.isActive) {
                TextButton(onClick = { onFiltersChange(ChoreFilters()) }) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }

        // Issue #74 CI fix: real (not contentPadding) bottom padding matching the Add-Chore FAB's
        // fixed footprint (56.dp ExtendedFloatingActionButton + 16.dp align/padding inset, plus
        // margin), so the LazyColumn is actually MEASURED/LAID OUT within a smaller region that
        // ends above the FAB's zone -- unlike LazyColumn's own contentPadding (which only adds
        // trailing scrollable space and does nothing for an already-in-viewport, never-scrolled
        // item), a real Modifier.padding here shrinks the box's real layout bounds, so even the
        // very first row's content can never render underneath the fixed FAB (which sits on top
        // in z-order and would otherwise intercept/steal those clicks).
        Box(modifier = Modifier.weight(1f).fillMaxSize().padding(bottom = 88.dp)) {
            when (val state = uiState) {
                is UiState.Idle, is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.chore_list_empty)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.data, key = { it.id }) { chore ->
                                ChoreRow(
                                    chore = chore,
                                    isCompleting = completingChoreId == chore.id,
                                    isPendingAction = pendingActionChoreId == chore.id,
                                    onCompleteClick = {
                                        if (chore.needsCompleterSelection) {
                                            choreAwaitingCompleter = chore
                                        } else {
                                            onComplete(chore, null)
                                        }
                                    },
                                    onSkip = { onSkip(chore) },
                                    onMarkDue = { onMarkDue(chore) },
                                    onDeleteClick = { choreAwaitingDelete = chore },
                                    onHistory = { onHistory(chore) },
                                    onEdit = { onEdit(chore) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Issue #70: extended FAB with an "Add Chore" text label alongside the icon, matching
        // web's icon+text button treatment (previously icon-only).
        ExtendedFloatingActionButton(
            onClick = onAddChore,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add_chore)) }
        )
    }

    if (showFiltersDialog) {
        ChoreFiltersDialog(
            filters = filters,
            availableAssignees = availableAssignees,
            availableScheduleTypes = availableScheduleTypes,
            availableAssignmentTypes = availableAssignmentTypes,
            onApply = { updated ->
                onFiltersChange(updated)
                showFiltersDialog = false
            },
            onDismiss = { showFiltersDialog = false }
        )
    }

    choreAwaitingCompleter?.let { chore ->
        // An "open" chore's eligiblePeople is an optional restriction (see ChoreFormScreen's
        // "Eligible people (optional)" section) -- empty means anyone can complete it, not that
        // no one can -- so fall back to every household member when it's unset.
        CompleterPickerDialog(
            people = chore.eligiblePeople.ifEmpty { availablePeople },
            onConfirm = { completedBy ->
                onComplete(chore, completedBy)
                choreAwaitingCompleter = null
            },
            onDismiss = { choreAwaitingCompleter = null }
        )
    }

    choreAwaitingDelete?.let { chore ->
        AlertDialog(
            onDismissRequest = { choreAwaitingDelete = null },
            title = { Text(stringResource(R.string.chore_delete_confirm_title)) },
            text = { Text(stringResource(R.string.chore_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    choreAwaitingDelete = null
                    onDelete(chore)
                }) { Text(stringResource(R.string.chore_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { choreAwaitingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ChoreRow(
    chore: Chore,
    isCompleting: Boolean,
    isPendingAction: Boolean,
    onCompleteClick: () -> Unit,
    onSkip: () -> Unit,
    onMarkDue: () -> Unit,
    onDeleteClick: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded }
    ) {
        // Issue #67: colored left accent bar + due-date color coding on the chore row, matching
        // ChoreCard.css's `.accent-bar` rules (red while due, muted gray once complete, no bar
        // otherwise). Uses drawBehind on this Column (not a sibling Box/IntrinsicSize.Min) since
        // that's a draw-time-only effect against the already-resolved layout size, with no
        // interaction with layout/semantics/click routing.
        val accentColor = statusAccentColor(chore)
        Column(
            modifier = Modifier
                .drawBehind {
                    if (accentColor != null) {
                        drawRect(color = accentColor, size = Size(4.dp.toPx(), size.height))
                    }
                }
                .padding(
                    start = if (accentColor != null) 20.dp else 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
        ) {
            Text(text = chore.name, style = MaterialTheme.typography.titleMedium)

            chore.currentAssignee?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            chore.nextDue?.let {
                Text(
                    text = stringResource(R.string.chore_next_due_format, formatNextDue(it)),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (expanded) {
                ChoreDetailSection(chore = chore)

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Start) {
                    if (isCompleting || isPendingAction) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    } else {
                        if (chore.isDue) {
                            Button(onClick = onCompleteClick) { Text(stringResource(R.string.complete_chore_button)) }
                            TextButton(onClick = onSkip) { Text(stringResource(R.string.chore_skip_action)) }
                        } else {
                            TextButton(onClick = onMarkDue) { Text(stringResource(R.string.chore_mark_due_action)) }
                        }
                        TextButton(onClick = onEdit) { Text(stringResource(R.string.chore_edit_action)) }
                        TextButton(onClick = onHistory) { Text(stringResource(R.string.chore_history_action)) }
                        TextButton(onClick = onDeleteClick) { Text(stringResource(R.string.chore_delete_action)) }
                    }
                }
            }
        }
    }
}

/**
 * Issue #67/#80: matches `ChoreCard.css`'s `.accent-bar` rules exactly -- red while due, muted
 * gray once complete (a distinct `state` value from "due"/"not_due"), no bar otherwise.
 */
@Composable
private fun statusAccentColor(chore: Chore): Color? = when {
    chore.isDue -> MaterialTheme.colorScheme.error
    chore.state == "complete" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> null
}

/**
 * Issue #31: formats a raw ISO `yyyy-MM-dd` `next_due` string as e.g. "Jul 2" (matching web's
 * `toLocaleDateString(undefined, { month: "short", day: "numeric" })`), falling back to the raw
 * string unchanged if it can't be parsed.
 */
private fun formatNextDue(raw: String): String {
    return try {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    } catch (e: DateTimeParseException) {
        raw
    }
}

/**
 * Issue #87: 2-column label/value grid (Status/Frequency, then Points/Assignee) framed by
 * dividers, matching `ChoreCard.css`'s `.expanded-meta`/`.meta-item`/`.meta-label`/`.meta-value`
 * -- replaces the single-column "Label: value" text stack. Deliberately uses
 * `Modifier.fillMaxWidth(0.5f)` per cell rather than `Modifier.weight(1f)` (see this file's/
 * issue #93's notes on this Row's history of unrelated Compose-testing fragility) -- same visual
 * result, without engaging `Row`'s weight-distribution machinery.
 */
@Composable
private fun ChoreDetailSection(chore: Chore) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_status_label),
                value = chore.state
            )
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_frequency_label),
                value = chore.scheduleSummary ?: "—"
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_points_label),
                value = chore.points.toString()
            )
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_assignee_label),
                value = chore.currentAssignee ?: stringResource(R.string.chore_completer_label)
            )
        }
        HorizontalDivider()
    }
}

/**
 * Issue #87: one 2-column grid cell -- an uppercase muted label over its value, matching
 * `ChoreCard.css`'s `.meta-label`/`.meta-value`.
 */
@Composable
private fun ChoreMetaItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
