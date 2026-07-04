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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.ui.UiState

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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("General Settings", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        value = draft.appTitle,
                        onValueChange = { draft = draft.copy(appTitle = it) },
                        label = { Text("App Title") }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        value = draft.timezone,
                        onValueChange = { draft = draft.copy(timezone = it) },
                        label = { Text("Timezone") }
                    )

                    if (saveState is UiState.Error) {
                        Text(saveState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }

                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = { onSave(draft) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}
