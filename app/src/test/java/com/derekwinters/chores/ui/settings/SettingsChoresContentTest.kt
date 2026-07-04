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
 * (area: android). Tests [SettingsChoresContent] directly.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsChoresContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun settingsChoresContent_dataLink_invokesNavCallback() {
        var navigated = false
        composeTestRule.setContent {
            SettingsChoresContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = {},
                onNavigateToData = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("Data (Export/Import, Points Log)").performClick()

        assert(navigated)
    }

    @Test
    fun settingsChoresContent_displaysSaveButton() {
        composeTestRule.setContent {
            SettingsChoresContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = {},
                onNavigateToData = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }
}
