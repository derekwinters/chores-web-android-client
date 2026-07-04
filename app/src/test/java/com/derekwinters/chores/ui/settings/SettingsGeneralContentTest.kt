package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #88 behavior 4: Each section screen has its own local draft and its own Save action
 * (area: android). Tests [SettingsGeneralContent] directly.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsGeneralContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsGeneralContent_editAppTitle_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores").toDomain()),
                saveState = UiState.Idle,
                onSave = { saved = it }
            )
        }

        composeTestRule.onNodeWithText("App Title").performTextClearance()
        composeTestRule.onNodeWithText("App Title").performTextInput("My House")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(saved?.appTitle == "My House")
    }

    @Test
    fun settingsGeneralContent_editTimezone_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores").toDomain()),
                saveState = UiState.Idle,
                onSave = { saved = it }
            )
        }

        // Issue #106: Timezone is now selected via dropdown with UTC offset labels
        // Default is UTC, so we need to click the button to open dropdown, then select a different timezone
        composeTestRule.onNodeWithText("UTC").performClick()  // Open dropdown with default UTC
        composeTestRule.onNodeWithText("UTC+01").performClick()  // Select a different timezone
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify that timezone was set to the selected UTC offset timezone
        assert(saved?.timezone == "Etc/GMT-1")  // UTC+01 maps to Etc/GMT-1
    }

    @Test
    fun settingsGeneralContent_displaysSaveButton() {
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }
}
