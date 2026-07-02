package com.derekwinters.chores.ui.theme

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.CurrentTheme
import com.derekwinters.chores.data.model.ThemeDefaultInfo
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #25 behavior: tapping a theme (or "Default") applies it immediately, no separate save
 * step (area: ui, android). Exercises [ThemePreferenceContent] directly (no Hilt needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ThemePreferenceContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun theme(id: String, name: String) = ThemeOption(
        id = id, name = name, background = "#000000", surface = "#111111",
        surface2 = "#222222", accent = "#333333", primary = "#444444", secondary = "#555555",
        success = "#0F0", warning = "#FF0", error = "#F00"
    )

    @Test
    fun themePreferenceContent_tapDefault_clearsOverride() {
        var selected: String? = "unset"
        val dark = theme("1", "Dark")
        composeTestRule.setContent {
            ThemePreferenceContent(
                uiState = UiState.Success(
                    ThemePreferenceData(
                        themes = listOf(dark, theme("2", "Light")),
                        current = CurrentTheme(theme = dark, isPersonalOverride = true),
                        defaultInfo = ThemeDefaultInfo(id = "1", name = "Dark")
                    )
                ),
                onSelectTheme = { selected = it }
            )
        }

        composeTestRule.onNodeWithText("Default (Dark)").performClick()

        assert(selected == null)
    }

    @Test
    fun themePreferenceContent_tapSpecificTheme_selectsItsId() {
        var selected: String? = null
        val dark = theme("1", "Dark")
        val light = theme("2", "Light")
        composeTestRule.setContent {
            ThemePreferenceContent(
                uiState = UiState.Success(
                    ThemePreferenceData(
                        themes = listOf(dark, light),
                        current = CurrentTheme(theme = dark, isPersonalOverride = false),
                        defaultInfo = ThemeDefaultInfo(id = "1", name = "Dark")
                    )
                ),
                onSelectTheme = { selected = it }
            )
        }

        composeTestRule.onNodeWithText("Light").performClick()

        assert(selected == "2")
    }

    @Test
    fun themePreferenceContent_defaultLabel_reflectsTrueDefaultEvenWhenOverridden() {
        // Regression: the "Default (name)" tile must show the real household default's name from
        // `defaultInfo`, not the currently-overridden theme's name, even while a personal override
        // (to a *different* theme than the default) is active.
        val dark = theme("1", "Dark")
        val pink = theme("3", "Pink")
        composeTestRule.setContent {
            ThemePreferenceContent(
                uiState = UiState.Success(
                    ThemePreferenceData(
                        themes = listOf(dark, pink),
                        current = CurrentTheme(theme = pink, isPersonalOverride = true),
                        defaultInfo = ThemeDefaultInfo(id = "1", name = "Dark")
                    )
                ),
                onSelectTheme = {}
            )
        }

        composeTestRule.onNodeWithText("Default (Dark)").assertExists()
    }
}
