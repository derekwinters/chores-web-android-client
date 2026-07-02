package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #20 behaviors: General/Auth/Chores/About sections and their nav entries (area: ui,
 * android). Exercises [SettingsContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsContent_editAppTitle_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto(app_title = "Chores").toDomain()),
                saveState = UiState.Idle,
                navActions = SettingsNavActions(),
                onSave = { saved = it },
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("App Title").performTextClearance()
        composeTestRule.onNodeWithText("App Title").performTextInput("My House")
        composeTestRule.onNodeWithText("Save Settings").performScrollTo().performClick()

        assert(saved?.appTitle == "My House")
    }

    @Test
    fun settingsContent_authEventLogLink_invokesNavCallback() {
        var navigated = false
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                navActions = SettingsNavActions(onNavigateToAuthLog = { navigated = true }),
                onSave = {},
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("Auth Event Log").performClick()

        assert(navigated)
    }

    @Test
    fun settingsContent_checkNow_invokesCallback() {
        var checked = false
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                navActions = SettingsNavActions(),
                onSave = {},
                onCheckForUpdates = { checked = true }
            )
        }

        composeTestRule.onNodeWithText("Check Now").performScrollTo().performClick()

        assert(checked)
    }
}
