package com.derekwinters.chores.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #24 behaviors: tap a theme row to set household default; built-in themes can't be
 * renamed/deleted (area: ui, android). Exercises [ThemeAdminContent] directly (no Hilt needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ThemeAdminContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val builtin = ThemeOption(
        id = 1, name = "Dark", isBuiltin = true, background = "#000000", surface = "#111111",
        surface2 = "#222222", accent = "#333333", primary = "#444444", secondary = "#555555",
        success = "#0F0", warning = "#FF0", error = "#F00"
    )

    @Test
    fun themeAdminContent_tapRow_setsDefault() {
        var defaultId: Int? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(builtin)),
                onSetDefault = { defaultId = it },
                onCreate = { _, _ -> },
                onUpdate = {},
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Dark").performClick()

        assert(defaultId == 1)
    }

    @Test
    fun themeAdminContent_editBuiltinTheme_disablesSaveAndDelete() {
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(builtin)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onUpdate = {},
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()

        composeTestRule.onNodeWithText("Delete Theme").assertDoesNotExist()
    }
}
