package com.derekwinters.chores.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.ui.UiState
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
    var selectedImportFilename by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            viewModel.exportConfig()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedImportFilename = uri.lastPathSegment
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
        selectedImportFilename = selectedImportFilename,
        exportState = exportState,
        logRetentionDays = logRetentionDays,
        importPreview = importPreview,
        importState = importState,
        onExportClick = { exportLauncher.launch("chores-backup.json") },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        onLogRetentionChange = viewModel::updateLogRetentionDays,
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onDismissImportResult = {
            viewModel.clearImportState()
            selectedImportFilename = null
        },
        onNavigateToPointsLog = navActions.onNavigateToPointsLog
    )
}

@Composable
fun DataSettingsContent(
    selectedImportFilename: String?,
    exportState: UiState<String>,
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
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Issue #114: Split into Export & Import section
            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            Text("Export & Import", style = MaterialTheme.typography.titleMedium)

            // Issue #117: Add description text
            Text(
                "Backup and restore your household data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onExportClick) { Text("Export Backup") }
                Button(onClick = onImportClick) { Text("Import Backup") }
            }

            // Issue #117: Show selected filename confirmation
            if (selectedImportFilename != null) {
                Text(
                    "Selected: $selectedImportFilename",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Issue #116, #117: Use banner for export/import results
            if (exportState is UiState.Success) {
                SettingsBanner(
                    message = "Backup exported successfully",
                    isError = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }

            if (importState is UiState.Success) {
                SettingsBanner(
                    message = "Data imported successfully",
                    isError = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            } else if (importState is UiState.Error) {
                SettingsBanner(
                    message = importState.message,
                    isError = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }

            // Issue #114: Log Retention section
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Log Retention", style = MaterialTheme.typography.titleMedium)

            Text(
                "Control how long activity logs are kept",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (logRetentionDays != null) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    value = logRetentionDays.toString(),
                    onValueChange = { value -> value.toIntOrNull()?.let(onLogRetentionChange) },
                    label = { Text("Days to keep log entries") }
                )
            }

            // Issue #114: Data Management section
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Data Management", style = MaterialTheme.typography.titleMedium)

            Text(
                "Access detailed activity records",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            TextButton(modifier = Modifier.padding(top = 16.dp).testTag("adminPointsLogButton"), onClick = onNavigateToPointsLog) {
                Text("Admin Points Log")
            }
        }
    }

    // Issue #119: Import confirmation dialog with bulleted breakdown
    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("Confirm Import") },
            text = {
                Column {
                    Text(
                        "This will import the following:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Issue #119: Bulleted list instead of single sentence
                    Text("• ${importPreview.peopleCount} people", style = MaterialTheme.typography.bodySmall)
                    Text("• ${importPreview.choresCount} chores", style = MaterialTheme.typography.bodySmall)
                    Text("• ${importPreview.settingsCount} settings", style = MaterialTheme.typography.bodySmall)

                    Text(
                        "This replaces all existing data and cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            },
            confirmButton = { TextButton(onClick = onConfirmImport) { Text("Import") } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("Cancel") } }
        )
    }
}
