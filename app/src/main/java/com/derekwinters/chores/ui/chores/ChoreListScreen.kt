package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.R
import com.derekwinters.chores.chores.Chore
import com.derekwinters.chores.common.UiState

/**
 * Chore list screen: renders all chores (read-only) with a Complete action per chore.
 * Create/edit/delete/skip/reassign/mark-due are out of scope for issue #5.
 *
 * Stateless so it can be unit tested without Hilt — the real ViewModel wiring lives in
 * [ChoreListRoute].
 */
@Composable
fun ChoreListScreen(
    modifier: Modifier = Modifier,
    uiState: UiState<List<Chore>>,
    onCompleteClicked: (Chore) -> Unit,
    onRetry: () -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingState(modifier)
        is UiState.Error -> ErrorState(modifier, uiState.message, onRetry)
        is UiState.Success -> if (uiState.data.isEmpty()) {
            EmptyState(modifier)
        } else {
            ChoreList(modifier, uiState.data, onCompleteClicked)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.chore_list_empty))
    }
}

@Composable
private fun ErrorState(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Button(modifier = Modifier.padding(top = 12.dp), onClick = onRetry) {
                Text(stringResource(R.string.chore_list_retry))
            }
        }
    }
}

@Composable
private fun ChoreList(
    modifier: Modifier,
    chores: List<Chore>,
    onCompleteClicked: (Chore) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(chores, key = { it.id }) { chore ->
            ChoreRow(chore = chore, onCompleteClicked = { onCompleteClicked(chore) })
        }
    }
}

@Composable
private fun ChoreRow(chore: Chore, onCompleteClicked: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = chore.name, style = MaterialTheme.typography.titleMedium)
            Text(text = chore.currentAssignee ?: stringResource(R.string.chore_needs_completer))
            Text(text = stringResource(R.string.chore_points_format, chore.points))
            Text(text = chore.state)
            chore.nextDue?.let { nextDue ->
                Text(text = stringResource(R.string.chore_next_due_format, nextDue))
            }
            Button(modifier = Modifier.padding(top = 8.dp), onClick = onCompleteClicked) {
                Text(stringResource(R.string.chore_complete_button))
            }
        }
    }
}
