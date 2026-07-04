package com.derekwinters.chores.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import kotlinx.coroutines.launch

/** Issue #22: cross-screen nav callback the Data settings destination needs. */
data class DataSettingsNavActions(val onNavigateToPointsLog: () -> Unit = {})

/**
 * Issue #22: config export/import backup and log-retention setting, plus a nav entry to the
 * Admin Points Log editor (issue #23).
 *
 * Thin Hilt-wired wrapper around [DataSettingsContent].
 */
@Composable
fun DataSettingsScreen(
    modifier: Modifier = Modifier,
    navActions: DataSettingsNavActions = DataSettingsNavActions(),
    viewModel: DataSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportState by viewModel.exportState.collectAsState()
    val importPreview by viewModel.importPreview.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val logRetentionDays by viewModel.logRetentionDays.collectAsState()

    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            viewModel.exportConfig()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    viewModel.previewImport(reader.readText())
                }
            }
        }
    }

    LaunchedEffect(exportState, pendingExportUri) {
        val json = (exportState as? UiState.Success)?.data
        val uri = pendingExportUri
        if (json != null && uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            pendingExportUri = null
            viewModel.clearExportState()
        }
    }

    DataSettingsContent(
        modifier = modifier,
        logRetentionDays = logRetentionDays,
        importPreview = importPreview,
        importState = importState,
        onExportClick = { exportLauncher.launch("chores-backup.json") },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        onLogRetentionChange = viewModel::updateLogRetentionDays,
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onDismissImportResult = viewModel::clearImportState,
        onNavigateToPointsLog = navActions.onNavigateToPointsLog
    )
}

@Composable
fun DataSettingsContent(
    logRetentionDays: Int?,
    importPreview: ImportPreview?,
    importState: UiState<com.derekwinters.chores.data.repository.ImportSummary>,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onLogRetentionChange: (Int) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onDismissImportResult: () -> Unit,
    onNavigateToPointsLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Divider(modifier = Modifier.padding(bottom = 16.dp))
        Text("Data", style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onExportClick) { Text("Export Backup") }
            Button(onClick = onImportClick) { Text("Import Backup") }
        }

        if (logRetentionDays != null) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                value = logRetentionDays.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let(onLogRetentionChange) },
                label = { Text("Days to keep log entries") }
            )
        }

        TextButton(modifier = Modifier.padding(top = 16.dp), onClick = onNavigateToPointsLog) {
            Text("Admin Points Log")
        }

        if (importState is UiState.Success) {
            SettingsBanner(
                message = "Imported ${importState.data.peopleCount} people, ${importState.data.choresCount} chores, " +
                    "${importState.data.settingsCount} settings",
                type = BannerType.SUCCESS,
                modifier = Modifier.padding(top = 16.dp)
            )
            TextButton(onClick = onDismissImportResult) { Text("OK") }
        } else if (importState is UiState.Error) {
            SettingsBanner(message = importState.message, type = BannerType.ERROR, modifier = Modifier.padding(top = 16.dp))
            TextButton(onClick = onDismissImportResult) { Text("OK") }
        }
    }

    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("Confirm Import") },
            text = {
                Column {
                    Text("${importPreview.peopleCount} people, ${importPreview.choresCount} chores, ${importPreview.settingsCount} settings")
                    Text(
                        "This replaces all existing data and cannot be undone.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = { TextButton(onClick = onConfirmImport) { Text("Import") } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("Cancel") } }
        )
    }
}
