package com.derekwinters.chores.ui.chores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Hilt-wired entry point for [ChoreListScreen], hosting the Completer-picker dialog when needed.
 * Replaces the Home tab's bootstrap "Hello World" content (issue #5).
 */
@Composable
fun ChoreListRoute(
    modifier: Modifier = Modifier,
    viewModel: ChoreListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingCompleterPick by viewModel.pendingCompleterPick.collectAsStateWithLifecycle()

    ChoreListScreen(
        modifier = modifier,
        uiState = uiState,
        onCompleteClicked = viewModel::onCompleteClicked,
        onRetry = viewModel::loadChores
    )

    pendingCompleterPick?.let { pending ->
        CompleterPickerDialog(
            choreName = pending.chore.name,
            people = pending.people,
            onConfirm = viewModel::onCompleterSelected,
            onDismiss = viewModel::onCompleterPickCancelled
        )
    }
}
