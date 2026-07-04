package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.UpdateCheckStatus
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #88 behavior 4: Each section screen has its own local draft and its own Save action
 * (area: android). Tests [SettingsAboutContent] directly.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsAboutContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsAboutContent_toggleUpdateCheckEnabled_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                updateStatus = null,
                onSave = { saved = it },
                onCheckForUpdates = {}
            )
        }

        // Click the entire row containing the toggle text and switch
        composeTestRule.onNodeWithText("Check for updates automatically").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        assert(saved?.updateCheckEnabled == true)
    }

    @Test
    fun settingsAboutContent_checkNow_invokesCallback() {
        var checked = false
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                updateStatus = null,
                onSave = {},
                onCheckForUpdates = { checked = true }
            )
        }

        composeTestRule.onNodeWithText("Check Now").performClick()

        assert(checked)
    }

    @Test
    fun settingsAboutContent_displaysVersionInfo() {
        val status = UpdateCheckStatus(
            currentVersion = "1.0.0",
            latestVersion = "1.0.1",
            lastCheckedAt = "2026-07-02T22:40:54.326377Z",
            checkEnabled = true,
            checkIntervalHours = 24,
            updateAvailable = true
        )
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                updateStatus = status,
                onSave = {},
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("Current version: 1.0.0").assertExists()
        composeTestRule.onNodeWithText("Latest version: 1.0.1").assertExists()
        composeTestRule.onNodeWithText("Update available!").assertExists()
    }

    @Test
    fun settingsAboutContent_displaysSaveButton() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                updateStatus = null,
                onSave = {},
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }
}
