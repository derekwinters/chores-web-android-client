package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #22 behaviors: import confirmation dialog with counts + "replaces all data" warning
 * (area: ui, android). Exercises [DataSettingsContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DataSettingsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dataSettingsContent_importPreview_confirmInvokesCallback() {
        var confirmed = false
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = ImportPreview(peopleCount = 2, choresCount = 5, settingsCount = 1, rawJson = "{}"),
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = { confirmed = true },
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("2 people, 5 chores, 1 settings", substring = true).assertExists()
        composeTestRule.onNodeWithText("This replaces all existing data and cannot be undone.").assertExists()

        composeTestRule.onNodeWithText("Import").performClick()

        assert(confirmed)
    }

    @Test
    fun dataSettingsContent_pointsLogLink_invokesCallback() {
        var navigated = false
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("Admin Points Log").performClick()

        assert(navigated)
    }

    @Test
    fun dataSettingsContent_displaysThreeDistinctSections() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Export & Import").assertExists()
        composeTestRule.onNodeWithText("Log Retention").assertExists()
        composeTestRule.onNodeWithText("Data Management").assertExists()
    }

    @Test
    fun dataSettingsContent_logRetentionSave_invokesCallback() {
        var saveClicked = false
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = { saveClicked = true },
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Save").performClick()

        assert(saveClicked)
    }

    @Test
    fun dataSettingsContent_selectedImportFilename_displaysConfirmation() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = "my_chores_backup.json",
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Selected: my_chores_backup.json").assertExists()
    }

    @Test
    fun dataSettingsContent_exportSuccess_displaysBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = "chores-backup.json",
                exportState = UiState.Success("{}"),
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Data exported successfully to: chores-backup.json").assertExists()
    }

    @Test
    fun dataSettingsContent_importSuccess_displaysBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Success(com.derekwinters.chores.data.repository.ImportSummary(peopleCount = 2, choresCount = 5, settingsCount = 1)),
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Imported 2 people, 5 chores, 1 settings", substring = true).assertExists()
    }

    @Test
    fun dataSettingsContent_importError_displaysBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Error("Import failed"),
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Import failed").assertExists()
    }
}
