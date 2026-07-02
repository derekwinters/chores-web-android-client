package com.derekwinters.chores.ui.log

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.ui.UiState

/**
 * Issue #19: chores-web's unified Activity Log — filters, pagination, and row-expand detail with
 * amendment diffs (old/new value for `updated` entries, `reassigned_to` for reassignments).
 *
 * Thin Hilt-wired wrapper around [ActivityLogContent].
 */
@Composable
fun ActivityLogScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()

    ActivityLogContent(
        modifier = modifier,
        uiState = uiState,
        filters = filters,
        onFiltersChange = viewModel::updateFilters,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage
    )
}

@Composable
fun ActivityLogContent(
    uiState: UiState<ActivityLogPageState>,
    filters: LogFilters,
    onFiltersChange: (LogFilters) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                value = filters.person.orEmpty(),
                onValueChange = { value -> onFiltersChange(filters.copy(person = value.ifBlank { null })) },
                label = { Text("Person") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                value = filters.chore.orEmpty(),
                onValueChange = { value -> onFiltersChange(filters.copy(chore = value.ifBlank { null })) },
                label = { Text("Chore") },
                singleLine = true
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (uiState) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
                is UiState.Success -> {
                    val page = uiState.data
                    if (page.entries.isEmpty()) {
                        Text(modifier = Modifier.align(Alignment.Center), text = "No activity found")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(page.entries, key = { it.id }) { entry -> LogRow(entry) }
                        }
                    }
                }
            }
        }

        if (uiState is UiState.Success) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onPreviousPage, enabled = uiState.data.page > 1) { Text("Previous") }
                Text("Page ${uiState.data.page} (${uiState.data.total} total)")
                TextButton(onClick = onNextPage) { Text("Next") }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${entry.action}: ${entry.targetName}", style = MaterialTheme.typography.titleSmall)
            Text("${entry.actor} · ${entry.timestamp}", style = MaterialTheme.typography.bodySmall)

            if (expanded) {
                entry.reassignedTo?.let { Text("Reassigned to: $it", style = MaterialTheme.typography.bodySmall) }
                if (entry.isAmendment) {
                    Text(
                        "${entry.fieldName}: ${entry.oldValue.orEmpty()} -> ${entry.newValue.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
