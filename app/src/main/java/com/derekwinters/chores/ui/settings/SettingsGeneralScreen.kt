package com.derekwinters.chores.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.SettingsBanner

/**
 * Issue #88: General settings section screen (independently-routed, shared SettingsViewModel
 * scoped to settings nav graph).
 */
@Composable
fun SettingsGeneralScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings"))
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    SettingsGeneralContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        onSave = viewModel::save
    )
}

// Issue #106: List of UTC-offset timezone options for picker
private val TIMEZONE_OPTIONS = listOf(
    "UTC-12" to "Etc/GMT+12",
    "UTC-11" to "Etc/GMT+11",
    "UTC-10" to "Etc/GMT+10",
    "UTC-09" to "Etc/GMT+9",
    "UTC-08" to "Etc/GMT+8",
    "UTC-07" to "Etc/GMT+7",
    "UTC-06" to "Etc/GMT+6",
    "UTC-05" to "Etc/GMT+5",
    "UTC-04" to "Etc/GMT+4",
    "UTC-03" to "Etc/GMT+3",
    "UTC-02" to "Etc/GMT+2",
    "UTC-01" to "Etc/GMT+1",
    "UTC" to "UTC",
    "UTC+01" to "Etc/GMT-1",
    "UTC+02" to "Etc/GMT-2",
    "UTC+03" to "Etc/GMT-3",
    "UTC+04" to "Etc/GMT-4",
    "UTC+05" to "Etc/GMT-5",
    "UTC+06" to "Etc/GMT-6",
    "UTC+07" to "Etc/GMT-7",
    "UTC+08" to "Etc/GMT-8",
    "UTC+09" to "Etc/GMT-9",
    "UTC+10" to "Etc/GMT-10",
    "UTC+11" to "Etc/GMT-11",
    "UTC+12" to "Etc/GMT-12"
)

@Composable
fun SettingsGeneralContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    onSave: (AppConfig) -> Unit,
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
                // Issue #96: Track dirty state (compare draft to original)
                val isDirty = draft != uiState.data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Issue #102: Divider before heading for App Title section
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                    Text("App Title", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("appTitleInput"),
                        value = draft.appTitle,
                        onValueChange = { draft = draft.copy(appTitle = it) },
                        label = { Text("App Title") }
                    )

                    // Issue #102: Divider before heading for Timezone section
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Timezone", style = MaterialTheme.typography.titleMedium)

                    // Issue #106: Replace free-text timezone with UTC-offset picker dropdown
                    TimezonePicker(
                        selectedTimezone = draft.timezone,
                        onTimezoneSelected = { draft = draft.copy(timezone = it) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )

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

                    // Issue #96: Disable save button when not dirty and show visual feedback
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
private fun TimezonePicker(
    selectedTimezone: String,
    onTimezoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = TIMEZONE_OPTIONS.find { it.second == selectedTimezone }?.first ?: selectedTimezone

    OutlinedButton(
        onClick = { expanded = true },
        modifier = modifier.testTag("timezonePickerButton")
    ) {
        Text(selectedLabel)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        TIMEZONE_OPTIONS.forEach { (label, value) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onTimezoneSelected(value)
                    expanded = false
                }
            )
        }
    }
}
