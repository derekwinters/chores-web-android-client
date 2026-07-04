package com.derekwinters.chores.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.SettingsBanner

/**
 * Issue #88: Chores settings section screen (independently-routed, shared SettingsViewModel scoped
 * to settings nav graph).
 */
@Composable
fun SettingsChoresScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings")),
    onNavigateToData: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    SettingsChoresContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        onSave = viewModel::save,
        onNavigateToData = onNavigateToData
    )
}

// Issue #112: List of hour labels for the due-hour dropdown
private val HOUR_OPTIONS = (0..23).map { hour ->
    val label = if (hour == 0) "Midnight" else if (hour == 12) "Noon" else {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour > 12) hour - 12 else hour
        "$displayHour $period"
    }
    label to hour
}

@Composable
fun SettingsChoresContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    onSave: (AppConfig) -> Unit,
    onNavigateToData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                var draft by remember(uiState.data) { mutableStateOf(uiState.data) }
                val isSaving = saveState is UiState.Loading
                // Issue #96: Track dirty state
                val isDirty = draft != uiState.data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Issue #102: Divider before heading
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                    Text("Chores", style = MaterialTheme.typography.titleMedium)

                    // Issue #112: Add description text
                    Text(
                        "Configure notification and scheduling preferences for chores",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        value = draft.dueSoonDays.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { draft = draft.copy(dueSoonDays = it) } },
                        label = { Text("Notify when due in N days") }
                    )

                    // Issue #112: Replace numeric field with dropdown for due-hour
                    DueHourPicker(
                        selectedHour = draft.dueTimeHour,
                        onHourSelected = { draft = draft.copy(dueTimeHour = it) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )

                    TextButton(onClick = onNavigateToData) { Text("Data (Export/Import, Points Log)") }

                    // Issue #116: Use banner for error messages
                    if (saveState is UiState.Error) {
                        SettingsBanner(
                            message = saveState.message,
                            isError = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }

                    // Issue #96: Disable save button when not dirty
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = { onSave(draft) },
                        enabled = !isSaving && isDirty
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DueHourPicker(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = HOUR_OPTIONS[selectedHour].first

    OutlinedButton(
        onClick = { expanded = true },
        modifier = modifier
    ) {
        Text("Mark chores due at: $selectedLabel")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        HOUR_OPTIONS.forEach { (label, hour) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onHourSelected(hour)
                    expanded = false
                }
            )
        }
    }
}
