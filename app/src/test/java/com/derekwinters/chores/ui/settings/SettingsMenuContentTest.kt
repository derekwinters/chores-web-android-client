package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #88 behavior 1: Settings destination shows a 6-row menu (General, Auth, Chores, Theme,
 * Data, About) instead of one scrolling form (area: ui). Exercises [SettingsMenuContent] directly.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsMenuContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsMenuContent_displays6MenuRows() {
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = {},
                onNavigateToAuth = {},
                onNavigateToChores = {},
                onNavigateToTheme = {},
                onNavigateToData = {},
                onNavigateToAbout = {}
            )
        }

        composeTestRule.onNodeWithText("General").assertExists()
        composeTestRule.onNodeWithText("Auth").assertExists()
        composeTestRule.onNodeWithText("Chores").assertExists()
        composeTestRule.onNodeWithText("Theme").assertExists()
        composeTestRule.onNodeWithText("Data").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun settingsMenuContent_generalRowClick_invokesNavigateToGeneral() {
        var navigated = false
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = { navigated = true },
                onNavigateToAuth = {},
                onNavigateToChores = {},
                onNavigateToTheme = {},
                onNavigateToData = {},
                onNavigateToAbout = {}
            )
        }

        composeTestRule.onNodeWithText("General").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_authRowClick_invokesNavigateToAuth() {
        var navigated = false
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = {},
                onNavigateToAuth = { navigated = true },
                onNavigateToChores = {},
                onNavigateToTheme = {},
                onNavigateToData = {},
                onNavigateToAbout = {}
            )
        }

        composeTestRule.onNodeWithText("Auth").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_choresRowClick_invokesNavigateToChores() {
        var navigated = false
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = {},
                onNavigateToAuth = {},
                onNavigateToChores = { navigated = true },
                onNavigateToTheme = {},
                onNavigateToData = {},
                onNavigateToAbout = {}
            )
        }

        composeTestRule.onNodeWithText("Chores").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_themeRowClick_invokesNavigateToTheme() {
        var navigated = false
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = {},
                onNavigateToAuth = {},
                onNavigateToChores = {},
                onNavigateToTheme = { navigated = true },
                onNavigateToData = {},
                onNavigateToAbout = {}
            )
        }

        composeTestRule.onNodeWithText("Theme").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_dataRowClick_invokesNavigateToData() {
        var navigated = false
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = {},
                onNavigateToAuth = {},
                onNavigateToChores = {},
                onNavigateToTheme = {},
                onNavigateToData = { navigated = true },
                onNavigateToAbout = {}
            )
        }

        composeTestRule.onNodeWithText("Data").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_aboutRowClick_invokesNavigateToAbout() {
        var navigated = false
        composeTestRule.setContent {
            SettingsMenuContent(
                onNavigateToGeneral = {},
                onNavigateToAuth = {},
                onNavigateToChores = {},
                onNavigateToTheme = {},
                onNavigateToData = {},
                onNavigateToAbout = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("About").performClick()

        assert(navigated)
    }
}
