package com.derekwinters.chores.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #24 behaviors: tap a theme row to set household default; the edit dialog renames a theme
 * (area: ui, android). Exercises [ThemeAdminContent] directly (no Hilt needed).
 *
 * The real API exposes no `is_builtin` flag, so unlike a prior version of this test, there's no
 * client-side "built-in themes can't be renamed/deleted" UI gating to exercise here — that
 * protection is enforced server-side only.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ThemeAdminContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val theme = ThemeOption(
        id = "1", name = "Dark", background = "#000000", surface = "#111111",
        surface2 = "#222222", accent = "#333333", primary = "#444444", secondary = "#555555",
        success = "#0F0", warning = "#FF0", error = "#F00"
    )

    @Test
    fun themeAdminContent_tapRow_setsDefault() {
        var defaultId: String? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = { defaultId = it },
                onCreate = { _, _ -> },
                onRename = { _, _ -> },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Dark").performClick()

        assert(defaultId == "1")
    }

    @Test
    fun themeAdminContent_editTheme_saveRenamesTheme() {
        var renamedId: String? = null
        var renamedTo: String? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onRename = { id, newName -> renamedId = id; renamedTo = newName },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Dark").performTextReplacement("Midnight")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(renamedId == "1")
        assert(renamedTo == "Midnight")
    }

    @Test
    fun themeAdminContent_editTheme_deleteConfirmDeletes() {
        var deletedId: String? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onRename = { _, _ -> },
                onDelete = { deletedId = it }
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Delete Theme").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        assert(deletedId == "1")
    }
}
