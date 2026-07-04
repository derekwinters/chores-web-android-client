package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
                selectedImportFilename = null,
                exportState = UiState.Idle,
                logRetentionDays = 90,
                importPreview = ImportPreview(peopleCount = 2, choresCount = 5, settingsCount = 1, rawJson = "{}"),
                importState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onConfirmImport = { confirmed = true },
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        // Issue #119: Check for bulleted breakdown instead of single line
        composeTestRule.onNodeWithText("• 2 people").assertExists()
        composeTestRule.onNodeWithText("• 5 chores").assertExists()
        composeTestRule.onNodeWithText("• 1 settings").assertExists()
        composeTestRule.onNodeWithText("This replaces all existing data and cannot be undone.").assertExists()

        composeTestRule.onNodeWithText("Import").performClick()

        assert(confirmed)
    }

    @Test
    fun dataSettingsContent_pointsLogLink_invokesCallback() {
        var navigated = false
        composeTestRule.setContent {
            DataSettingsContent(
                selectedImportFilename = null,
                exportState = UiState.Idle,
                logRetentionDays = 90,
                importPreview = null,
                importState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = { navigated = true }
            )
        }

        composeTestRule.onNodeWithTag("adminPointsLogButton").performClick()

        assert(navigated)
    }
}
